package com.waterai.consultant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterai.consultant.chat.ChatService;
import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "deepseek")
public class DeepSeekLlmClient implements LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxTokens;
    private final String apiKey;

    public DeepSeekLlmClient(ObjectMapper objectMapper,
                             @Value("${app.llm.deepseek.base-url}") String baseUrl,
                             @Value("${app.llm.deepseek.api-key}") String apiKey,
                             @Value("${app.llm.deepseek.model}") String model,
                             @Value("${app.llm.deepseek.max-tokens}") int maxTokens) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.maxTokens = maxTokens;
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String answer(String prompt, String question, String mode, List<KnowledgeEvidence> evidences) {
        if (evidences.isEmpty()) {
            return ChatService.INSUFFICIENT_ANSWER;
        }
        return chat(prompt, false);
    }

    @Override
    public String analyzeRequirement(String prompt, String requirementDesc, String moduleName, List<KnowledgeEvidence> evidences) {
        if (evidences.isEmpty()) {
            return """
                    {"requirement_understanding":"%s","feasibility_level":"D","conclusion":"当前资料不足，无法确认。建议补充相关项目文档、能力清单或历史需求案例。","missing_capabilities":["缺少可检索依据"],"impact_modules":[],"risk_points":["资料不足，不能评估实现风险"],"recommended_solution":"请补充系统能力清单、页面说明、接口说明或历史需求案例后再判断。","workload_level":"未知"}
                    """.formatted(escapeJson(requirementDesc));
        }
        return normalizeJson(chat(prompt, true));
    }

    private String chat(String prompt, boolean jsonMode) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "DeepSeek API Key 未配置");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt(jsonMode)),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("stream", false);
        body.put("max_tokens", maxTokens);
        body.put("thinking", Map.of("type", "disabled"));
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        try {
            String response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("DeepSeek 返回内容为空");
            }
            return content.trim();
        } catch (Exception ex) {
            // 模型调用失败时明确提示，不伪造答案；引用仍由上层按检索结果返回。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek 调用失败：" + ex.getMessage());
        }
    }

    private String systemPrompt(boolean jsonMode) {
        if (jsonMode) {
            return "你是 AI 水务项目智能顾问。只基于用户提供的 evidence 回答。输出严格 JSON，不要 Markdown，不要代码块。";
        }
        return "你是 AI 水务项目智能顾问。只基于用户提供的 evidence 回答；没有依据时必须回答“当前资料不足，无法确认。建议补充相关项目文档、能力清单或历史需求案例。”。";
    }

    private String normalizeJson(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://api.deepseek.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
