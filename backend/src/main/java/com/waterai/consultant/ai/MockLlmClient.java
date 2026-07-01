package com.waterai.consultant.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterai.consultant.chat.ChatService;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    private final ObjectMapper objectMapper;

    public MockLlmClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String answer(String prompt, String question, String mode, List<KnowledgeEvidence> evidences) {
        if (evidences.isEmpty()) {
            return ChatService.INSUFFICIENT_ANSWER;
        }

        KnowledgeEvidence first = evidences.getFirst();
        return "基于当前项目资料，初步判断如下："
                + summarize(first.content())
                + "。主要依据来自《" + first.sourceTitle() + "》。如需更精确结论，请继续补充相关项目文档、页面说明或业务规则。";
    }

    @Override
    public String analyzeRequirement(String prompt, String requirementDesc, String moduleName, List<KnowledgeEvidence> evidences) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requirement_understanding", requirementDesc);
        result.put("feasibility_level", evidences.isEmpty() ? "D" : inferFeasibility(evidences));
        result.put("conclusion", evidences.isEmpty()
                ? ChatService.INSUFFICIENT_ANSWER
                : "已根据能力清单、历史需求案例及相关结构化资料完成初步判断。");
        result.put("missing_capabilities", evidences.isEmpty() ? List.of("缺少可检索依据") : List.of());
        result.put("impact_modules", moduleName == null || moduleName.isBlank() ? List.of() : List.of(moduleName));
        result.put("risk_points", evidences.isEmpty()
                ? List.of("资料不足，不能评估实现风险")
                : List.of("需结合现场配置、接口联调和数据表字段进一步确认"));
        result.put("recommended_solution", evidences.isEmpty()
                ? "请补充系统能力清单、页面说明、接口说明或历史需求案例后再判断。"
                : "优先复用现有能力和历史案例；如能力清单显示需配置，则先走配置验证，再评估是否二次开发。");
        result.put("workload_level", evidences.isEmpty() ? "未知" : inferWorkload(evidences));

        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            return "{\"feasibility_level\":\"D\",\"conclusion\":\"当前资料不足，无法确认\"}";
        }
    }

    private String inferFeasibility(List<KnowledgeEvidence> evidences) {
        String joined = evidences.stream().map(KnowledgeEvidence::content).reduce("", (left, right) -> left + " " + right);
        if (joined.contains("unsupported") || joined.contains("不建议")) return "E";
        if (joined.contains("custom_required") || joined.contains("二次开发")) return "C";
        if (joined.contains("configurable") || joined.contains("配置")) return "B";
        return "A";
    }

    private String inferWorkload(List<KnowledgeEvidence> evidences) {
        String joined = evidences.stream().map(KnowledgeEvidence::content).reduce("", (left, right) -> left + " " + right);
        if (joined.contains("二次开发") || joined.contains("custom_required")) return "高";
        if (joined.contains("配置") || joined.contains("联调")) return "中";
        return "低";
    }

    private String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "检索到了相关资料，但内容摘要为空";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 220 ? normalized.substring(0, 220) + "..." : normalized;
    }
}
