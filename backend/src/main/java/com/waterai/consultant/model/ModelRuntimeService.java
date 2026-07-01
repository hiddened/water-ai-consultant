package com.waterai.consultant.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterai.consultant.ai.LlmClient;
import com.waterai.consultant.chat.ChatService;
import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import com.waterai.consultant.embedding.EmbeddingClient;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ModelRuntimeService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final LlmClient fallbackLlmClient;
    private final EmbeddingClient fallbackEmbeddingClient;
    private final String envProvider;
    private final String envDeepSeekBaseUrl;
    private final String envDeepSeekModel;

    public ModelRuntimeService(NamedParameterJdbcTemplate jdbcTemplate,
                               ObjectMapper objectMapper,
                               LlmClient fallbackLlmClient,
                               EmbeddingClient fallbackEmbeddingClient,
                               @Value("${app.llm.provider}") String envProvider,
                               @Value("${app.llm.deepseek.base-url}") String envDeepSeekBaseUrl,
                               @Value("${app.llm.deepseek.model}") String envDeepSeekModel) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.fallbackLlmClient = fallbackLlmClient;
        this.fallbackEmbeddingClient = fallbackEmbeddingClient;
        this.envProvider = envProvider;
        this.envDeepSeekBaseUrl = envDeepSeekBaseUrl;
        this.envDeepSeekModel = envDeepSeekModel;
    }

    public ModelMetadata chatMetadata() {
        return defaultConfig("chat")
                .map(config -> new ModelMetadata(toUuid(config.get("id")), string(config.get("provider")), string(config.get("model_name"))))
                .orElse(new ModelMetadata(null, envProvider, fallbackModelName()));
    }

    public ModelInvocationResult answer(String systemPrompt,
                                        String userPrompt,
                                        String question,
                                        String mode,
                                        List<KnowledgeEvidence> evidences) {
        Map<String, Object> config = defaultConfig("chat").orElse(null);
        if (config == null) {
            String content = fallbackLlmClient.answer(systemPrompt + "\n\n" + userPrompt, question, mode, evidences);
            return new ModelInvocationResult(null, envProvider, fallbackModelName(), content);
        }
        String content = invokeChatConfig(config, systemPrompt, userPrompt, false);
        return new ModelInvocationResult(toUuid(config.get("id")), string(config.get("provider")), string(config.get("model_name")), content);
    }

    public ModelInvocationResult analyzeRequirement(String systemPrompt,
                                                    String userPrompt,
                                                    String requirementDesc,
                                                    String moduleName,
                                                    List<KnowledgeEvidence> evidences) {
        Map<String, Object> config = defaultConfig("chat").orElse(null);
        if (config == null) {
            String content = fallbackLlmClient.analyzeRequirement(systemPrompt + "\n\n" + userPrompt, requirementDesc, moduleName, evidences);
            return new ModelInvocationResult(null, envProvider, fallbackModelName(), content);
        }
        String content = invokeChatConfig(config, systemPrompt, userPrompt, true);
        return new ModelInvocationResult(toUuid(config.get("id")), string(config.get("provider")), string(config.get("model_name")), content);
    }

    public List<Double> embedText(String text) {
        Map<String, Object> config = defaultConfig("embedding").orElse(null);
        if (config == null) {
            return fallbackEmbeddingClient.embedText(text);
        }
        if ("mock".equals(string(config.get("provider")))) {
            return mockEmbedding(text, intValue(config.get("dimension"), fallbackEmbeddingClient.dimension()));
        }
        return invokeEmbeddingConfig(config, List.of(text)).getFirst();
    }

    public String embeddingProvider() {
        return defaultConfig("embedding").map(config -> string(config.get("provider"))).orElse(fallbackEmbeddingClient.provider());
    }

    public int embeddingDimension() {
        return defaultConfig("embedding").map(config -> intValue(config.get("dimension"), fallbackEmbeddingClient.dimension())).orElse(fallbackEmbeddingClient.dimension());
    }

    public boolean realEmbedding() {
        return defaultConfig("embedding")
                .map(config -> !"mock".equals(string(config.get("provider"))))
                .orElse(fallbackEmbeddingClient.realEmbedding());
    }

    public Map<String, Object> testConfig(Map<String, Object> config) {
        String modelType = string(config.get("model_type"));
        if ("chat".equals(modelType)) {
            String content = invokeChatConfig(config, "你是连接测试助手，只返回一句简短中文。", "请回答：模型连接正常。", false);
            return Map.of("success", true, "message", "模型连接正常", "sample", content);
        }
        if ("embedding".equals(modelType)) {
            List<Double> vector = "mock".equals(string(config.get("provider")))
                    ? mockEmbedding("水务模型配置测试", intValue(config.get("dimension"), fallbackEmbeddingClient.dimension()))
                    : invokeEmbeddingConfig(config, List.of("水务模型配置测试")).getFirst();
            return Map.of("success", true, "message", "Embedding 连接正常", "dimension", vector.size());
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "model_type 不支持");
    }

    private java.util.Optional<Map<String, Object>> defaultConfig(String modelType) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT *
                FROM ai_model_config
                WHERE deleted = FALSE AND enabled = TRUE AND default_config = TRUE AND model_type = :model_type
                ORDER BY updated_at DESC
                LIMIT 1
                """, new MapSqlParameterSource("model_type", modelType), new ColumnMapRowMapper());
        return rows.stream().findFirst();
    }

    private String invokeChatConfig(Map<String, Object> config, String systemPrompt, String userPrompt, boolean jsonMode) {
        String provider = string(config.get("provider"));
        if ("mock".equals(provider)) {
            return jsonMode
                    ? "{\"feasibility_level\":\"D\",\"conclusion\":\"Mock 模型连接正常。\"}"
                    : "Mock 模型连接正常。";
        }
        String apiKey = string(config.get("api_key_value"));
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型 API Key 未配置");
        }
        String baseUrl = trimTrailingSlash(string(config.get("base_url")));
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "deepseek".equals(provider) ? trimTrailingSlash(envDeepSeekBaseUrl) : null;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型 Base URL 未配置");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", string(config.get("model_name")));
        body.put("messages", List.of(
                Map.of("role", "system", "content", nullToEmpty(systemPrompt)),
                Map.of("role", "user", "content", nullToEmpty(userPrompt))
        ));
        body.put("stream", false);
        Integer maxTokens = intObject(config.get("max_tokens"));
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }
        BigDecimal temperature = decimal(config.get("temperature"));
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        if ("deepseek".equals(provider)) {
            body.put("thinking", Map.of("type", "disabled"));
        }

        try {
            String response = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            String content = objectMapper.readTree(response).path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("模型返回内容为空");
            }
            return stripJsonFence(content.trim());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            // 不记录 API Key，只把上游错误压缩为可读信息返回给调用方。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模型调用失败：" + ex.getMessage());
        }
    }

    private List<List<Double>> invokeEmbeddingConfig(Map<String, Object> config, List<String> texts) {
        String apiKey = string(config.get("api_key_value"));
        String baseUrl = trimTrailingSlash(string(config.get("base_url")));
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Embedding API Key 未配置");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Embedding Base URL 未配置");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", string(config.get("model_name")));
        body.put("input", texts);
        try {
            String response = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode data = objectMapper.readTree(response).path("data");
            List<List<Double>> result = new ArrayList<>();
            for (JsonNode item : data) {
                List<Double> vector = new ArrayList<>();
                for (JsonNode value : item.path("embedding")) {
                    vector.add(value.asDouble());
                }
                int expectedDimension = intValue(config.get("dimension"), vector.size());
                if (vector.size() != expectedDimension) {
                    throw new IllegalStateException("Embedding 维度不匹配，期望 " + expectedDimension + "，实际 " + vector.size());
                }
                result.add(vector);
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Embedding 调用失败：" + ex.getMessage());
        }
    }

    private List<Double> mockEmbedding(String text, int dimension) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] seed = digest.digest(nullToEmpty(text).getBytes(StandardCharsets.UTF_8));
            List<Double> vector = new ArrayList<>(dimension);
            for (int i = 0; i < dimension; i++) {
                int value = seed[i % seed.length] & 0xff;
                vector.add((value / 127.5d) - 1.0d);
            }
            return vector;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Mock embedding 生成失败");
        }
    }

    private String fallbackModelName() {
        return "deepseek".equals(envProvider) ? envDeepSeekModel : "mock";
    }

    private String stripJsonFence(String content) {
        if (content.startsWith("```")) {
            return content.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        return content;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private UUID toUuid(Object value) {
        return value == null ? null : UUID.fromString(String.valueOf(value));
    }

    private Integer intObject(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }

    private int intValue(Object value, int defaultValue) {
        return value == null ? defaultValue : ((Number) value).intValue();
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
