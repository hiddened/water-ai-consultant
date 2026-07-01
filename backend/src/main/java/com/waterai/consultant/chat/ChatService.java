package com.waterai.consultant.chat;

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
import java.util.List;
import java.util.Set;

@Service
public class ChatService {

    private static final Set<String> MODES = Set.of("doc_qa", "page_help", "business_qa", "requirement_check");
    public static final String INSUFFICIENT_ANSWER = "当前资料不足，无法确认。建议补充相关项目文档、能力清单或历史需求案例。";

    private final KnowledgeRetrievalService retrievalService;
    private final PromptTemplateService promptTemplateService;
    private final ModelRuntimeService modelRuntimeService;
    private final ChatPersistenceService persistenceService;
    private final TraceIdProvider traceIdProvider;

    public ChatService(KnowledgeRetrievalService retrievalService,
                       PromptTemplateService promptTemplateService,
                       ModelRuntimeService modelRuntimeService,
                       ChatPersistenceService persistenceService,
                       TraceIdProvider traceIdProvider) {
        this.retrievalService = retrievalService;
        this.promptTemplateService = promptTemplateService;
        this.modelRuntimeService = modelRuntimeService;
        this.persistenceService = persistenceService;
        this.traceIdProvider = traceIdProvider;
    }

    public ChatResponse chat(ChatRequest request) {
        if (request.projectId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "project_id 不能为空");
        }
        if (!MODES.contains(request.mode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "mode 不支持");
        }

        List<KnowledgeEvidence> evidences = retrievalService.retrieve(request.projectId(), request.question(), request.mode(), 8);
        if (missingSpecificCapability(request.question(), evidences)) {
            // 对明确专有能力必须有同词依据，避免“巡检”误匹配成“无人机巡检”这类过度泛化结论。
            evidences = List.of();
        }
        PromptRenderResult prompt = promptTemplateService.renderChat(request.projectId(), request.mode(), request.question(), evidences);
        ModelMetadata modelMetadata = modelRuntimeService.chatMetadata();
        ModelInvocationResult modelResult = evidences.isEmpty()
                ? new ModelInvocationResult(modelMetadata.modelConfigId(), modelMetadata.provider(), modelMetadata.modelName(), INSUFFICIENT_ANSWER)
                : modelRuntimeService.answer(prompt.systemPrompt(), prompt.userPrompt(), request.question(), request.mode(), evidences);
        String answer = modelResult.content();
        if (answer != null && answer.contains("当前资料不足，无法确认")) {
            answer = INSUFFICIENT_ANSWER;
        }
        BigDecimal confidence = evidences.isEmpty() ? BigDecimal.valueOf(0.10) : BigDecimal.valueOf(0.76);
        String traceId = traceIdProvider.currentTraceId();
        boolean insufficientAnswer = answer != null && answer.contains("当前资料不足，无法确认");
        String searchStrategy = searchStrategy(evidences);

        ConversationSaveResult saveResult = persistenceService.saveConversation(
                request.projectId(),
                request.mode(),
                request.question(),
                answer,
                confidence,
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

        return new ChatResponse(
                answer,
                evidences.stream().map(AnswerReferenceDto::from).toList(),
                related(evidences, "PAGE"),
                related(evidences, "CAPABILITY"),
                related(evidences, "API"),
                confidence,
                traceId,
                saveResult.sessionId(),
                saveResult.assistantMessageId(),
                modelResult.provider(),
                modelResult.provider(),
                modelResult.modelName(),
                prompt.templateId(),
                prompt.templateName(),
                searchStrategy,
                insufficientAnswer
        );
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

    private boolean missingSpecificCapability(String question, List<KnowledgeEvidence> evidences) {
        for (String term : List.of("无人机", "区块链", "药耗优化算法")) {
            if (question != null && question.contains(term)
                    && evidences.stream().noneMatch(evidence -> evidence.content() != null && evidence.content().contains(term))) {
                return true;
            }
        }
        return false;
    }

    private List<RelatedItem> related(List<KnowledgeEvidence> evidences, String sourceType) {
        return evidences.stream()
                .filter(evidence -> sourceType.equals(evidence.sourceType()))
                .map(evidence -> new RelatedItem(evidence.sourceId(), evidence.sourceTitle(), evidence.sourceLocator()))
                .limit(5)
                .toList();
    }
}
