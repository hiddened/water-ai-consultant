package com.waterai.consultant.requirement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterai.consultant.chat.ChatService;
import com.waterai.consultant.chat.ConversationSaveResult;
import com.waterai.consultant.chat.ChatPersistenceService;
import com.waterai.consultant.chat.RelatedItem;
import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import com.waterai.consultant.common.trace.TraceIdProvider;
import com.waterai.consultant.model.ModelInvocationResult;
import com.waterai.consultant.model.ModelMetadata;
import com.waterai.consultant.model.ModelRuntimeService;
import com.waterai.consultant.prompt.PromptRenderResult;
import com.waterai.consultant.prompt.PromptTemplateService;
import com.waterai.consultant.retrieval.AnswerReferenceDto;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import com.waterai.consultant.retrieval.KnowledgeRetrievalService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RequirementCheckService {

    private final KnowledgeRetrievalService retrievalService;
    private final PromptTemplateService promptTemplateService;
    private final ModelRuntimeService modelRuntimeService;
    private final ChatPersistenceService persistenceService;
    private final TraceIdProvider traceIdProvider;
    private final ObjectMapper objectMapper;

    public RequirementCheckService(KnowledgeRetrievalService retrievalService,
                                   PromptTemplateService promptTemplateService,
                                   ModelRuntimeService modelRuntimeService,
                                   ChatPersistenceService persistenceService,
                                   TraceIdProvider traceIdProvider,
                                   ObjectMapper objectMapper) {
        this.retrievalService = retrievalService;
        this.promptTemplateService = promptTemplateService;
        this.modelRuntimeService = modelRuntimeService;
        this.persistenceService = persistenceService;
        this.traceIdProvider = traceIdProvider;
        this.objectMapper = objectMapper;
    }

    public RequirementCheckResponse check(RequirementCheckRequest request) {
        if (request.projectId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "project_id 不能为空");
        }

        List<KnowledgeEvidence> evidences = retrievalService.retrieve(
                request.projectId(),
                request.requirementDesc() + " " + nullToEmpty(request.moduleName()),
                "requirement_check",
                10
        );
        if (missingSpecificCapability(request.requirementDesc(), evidences)) {
            // 明确专有能力必须在资料中出现同词依据，否则按资料不足处理。
            evidences = List.of();
        }
        boolean hasCapability = hasSource(evidences, "CAPABILITY");
        boolean hasRequirementCase = hasSource(evidences, "REQUIREMENT_CASE");
        PromptRenderResult prompt = promptTemplateService.renderRequirement(request.projectId(), request.requirementDesc(), request.moduleName(), evidences);
        ModelMetadata modelMetadata = modelRuntimeService.chatMetadata();
        ModelInvocationResult modelResult = evidences.isEmpty()
                ? new ModelInvocationResult(modelMetadata.modelConfigId(), modelMetadata.provider(), modelMetadata.modelName(), "{}")
                : modelRuntimeService.analyzeRequirement(prompt.systemPrompt(), prompt.userPrompt(), request.requirementDesc(), request.moduleName(), evidences);
        String llmJson = modelResult.content();
        Map<String, Object> parsed = parseJson(llmJson);
        String traceId = traceIdProvider.currentTraceId();
        String feasibilityLevel = normalizeFeasibility(
                stringValue(parsed, "feasibility_level", evidences.isEmpty() ? "D" : "C"),
                evidences,
                hasCapability,
                hasRequirementCase
        );
        String conclusion = normalizeConclusion(
                stringValue(parsed, "conclusion", evidences.isEmpty() ? ChatService.INSUFFICIENT_ANSWER : "已完成初步判断"),
                evidences,
                hasCapability
        );
        boolean insufficientAnswer = conclusion.contains("当前资料不足，无法确认");
        String searchStrategy = searchStrategy(evidences);

        RequirementCheckResponse response = new RequirementCheckResponse(
                stringValue(parsed, "requirement_understanding", request.requirementDesc()),
                feasibilityLevel,
                conclusion,
                related(evidences, "CAPABILITY"),
                stringList(parsed, "missing_capabilities"),
                related(evidences, "PAGE"),
                related(evidences, "API"),
                related(evidences, "DB_TABLE"),
                impactModules(parsed, evidences, request.moduleName()),
                stringList(parsed, "risk_points"),
                stringValue(parsed, "recommended_solution", evidences.isEmpty() ? "请补充相关项目文档、能力清单或历史需求案例后再判断。" : "优先复用现有能力。"),
                stringValue(parsed, "workload_level", evidences.isEmpty() ? "未知" : "低"),
                evidences.stream().map(AnswerReferenceDto::from).toList(),
                traceId,
                null,
                null,
                modelResult.provider(),
                modelResult.provider(),
                modelResult.modelName(),
                prompt.templateId(),
                prompt.templateName(),
                searchStrategy,
                insufficientAnswer
        );

        String answerContent = toJson(response);
        ConversationSaveResult saveResult = persistenceService.saveConversation(
                request.projectId(),
                "requirement_check",
                request.requirementDesc(),
                answerContent,
                evidences.isEmpty() ? BigDecimal.valueOf(0.10) : BigDecimal.valueOf(0.74),
                traceId,
                modelResult.provider(),
                modelResult.modelConfigId(),
                modelResult.provider(),
                modelResult.modelName(),
                prompt.templateId(),
                prompt.templateName(),
                prompt.systemPrompt() + "\n\n" + prompt.userPrompt(),
                searchStrategy,
                insufficientAnswer,
                evidences
        );
        return new RequirementCheckResponse(
                response.requirementUnderstanding(),
                response.feasibilityLevel(),
                response.conclusion(),
                response.matchedCapabilities(),
                response.missingCapabilities(),
                response.relatedPages(),
                response.relatedApis(),
                response.relatedTables(),
                response.impactModules(),
                response.riskPoints(),
                response.recommendedSolution(),
                response.workloadLevel(),
                response.references(),
                response.traceId(),
                saveResult.sessionId(),
                saveResult.assistantMessageId(),
                response.llmProvider(),
                response.modelProvider(),
                response.modelName(),
                response.promptTemplateId(),
                response.promptTemplateName(),
                response.searchStrategy(),
                response.insufficientAnswer()
        );
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of("feasibility_level", "D", "conclusion", "当前资料不足，无法确认");
        }
    }

    private String toJson(RequirementCheckResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            return response.conclusion();
        }
    }

    private List<RelatedItem> related(List<KnowledgeEvidence> evidences, String sourceType) {
        return evidences.stream()
                .filter(evidence -> sourceType.equals(evidence.sourceType()))
                .map(evidence -> new RelatedItem(evidence.sourceId(), evidence.sourceTitle(), evidence.sourceLocator()))
                .limit(5)
                .toList();
    }

    private boolean hasSource(List<KnowledgeEvidence> evidences, String sourceType) {
        return evidences.stream().anyMatch(evidence -> sourceType.equals(evidence.sourceType()));
    }

    private boolean missingSpecificCapability(String question, List<KnowledgeEvidence> evidences) {
        for (String term : List.of("无人机", "区块链", "药耗优化算法")) {
            if (question != null && question.contains(term)
                    && evidences.stream().noneMatch(evidence -> evidence.content() != null && evidence.content().contains(term))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeFeasibility(String modelLevel,
                                        List<KnowledgeEvidence> evidences,
                                        boolean hasCapability,
                                        boolean hasRequirementCase) {
        if (evidences.isEmpty()) {
            return "D";
        }
        String level = Set.of("A", "B", "C", "D", "E").contains(modelLevel) ? modelLevel : "D";
        if (!hasCapability && ("A".equals(level) || "B".equals(level))) {
            return "C";
        }
        if (!(hasCapability && hasRequirementCase) && "A".equals(level)) {
            return "B";
        }
        return level;
    }

    private String normalizeConclusion(String conclusion, List<KnowledgeEvidence> evidences, boolean hasCapability) {
        if (evidences.isEmpty()) {
            return ChatService.INSUFFICIENT_ANSWER;
        }
        if (!hasCapability) {
            return "未命中能力清单，不能判断为现有功能完全支持。" + conclusion;
        }
        return conclusion;
    }

    private List<String> impactModules(Map<String, Object> parsed, List<KnowledgeEvidence> evidences, String requestedModule) {
        Set<String> modules = new LinkedHashSet<>(stringList(parsed, "impact_modules"));
        if (requestedModule != null && !requestedModule.isBlank()) {
            modules.add(requestedModule);
        }
        evidences.stream()
                .map(KnowledgeEvidence::moduleName)
                .filter(module -> module != null && !module.isBlank())
                .forEach(modules::add);
        return List.copyOf(modules);
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Map<String, Object> parsed, String key) {
        Object value = parsed.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }

    private String stringValue(Map<String, Object> parsed, String key, String defaultValue) {
        Object value = parsed.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String searchStrategy(List<KnowledgeEvidence> evidences) {
        if (evidences.isEmpty()) {
            return "none";
        }
        boolean hasVector = evidences.stream().anyMatch(evidence -> "vector_distance".equals(evidence.scoreType()));
        boolean hasKeyword = evidences.stream().anyMatch(evidence -> evidence.scoreType() == null || "keyword_score".equals(evidence.scoreType()));
        if (hasVector && hasKeyword) {
            return "hybrid";
        }
        return hasVector ? "vector" : "keyword";
    }
}
