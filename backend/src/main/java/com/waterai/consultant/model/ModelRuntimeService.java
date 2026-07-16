package com.waterai.consultant.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterai.consultant.ai.SpringAiModelFactory;
import com.waterai.consultant.chat.ChatService;
import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ModelRuntimeService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SpringAiModelFactory modelFactory;
    private final String envProvider;
    private final String envDeepSeekBaseUrl;
    private final String envDeepSeekApiKey;
    private final String envDeepSeekModel;
    private final int envDeepSeekMaxTokens;
    private final String envEmbeddingProvider;
    private final String envEmbeddingBaseUrl;
    private final String envEmbeddingApiKey;
    private final String envEmbeddingModel;
    private final int envEmbeddingDimension;

    public ModelRuntimeService(NamedParameterJdbcTemplate jdbcTemplate,
                               ObjectMapper objectMapper,
                               SpringAiModelFactory modelFactory,
                               @Value("${app.llm.provider}") String envProvider,
                               @Value("${app.llm.deepseek.base-url}") String envDeepSeekBaseUrl,
                               @Value("${app.llm.deepseek.api-key}") String envDeepSeekApiKey,
                               @Value("${app.llm.deepseek.model}") String envDeepSeekModel,
                               @Value("${app.llm.deepseek.max-tokens}") int envDeepSeekMaxTokens,
                               @Value("${app.embedding.provider}") String envEmbeddingProvider,
                               @Value("${app.embedding.base-url}") String envEmbeddingBaseUrl,
                               @Value("${app.embedding.api-key}") String envEmbeddingApiKey,
                               @Value("${app.embedding.model}") String envEmbeddingModel,
                               @Value("${app.embedding.dimension}") int envEmbeddingDimension) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.modelFactory = modelFactory;
        this.envProvider = envProvider;
        this.envDeepSeekBaseUrl = envDeepSeekBaseUrl;
        this.envDeepSeekApiKey = envDeepSeekApiKey;
        this.envDeepSeekModel = envDeepSeekModel;
        this.envDeepSeekMaxTokens = envDeepSeekMaxTokens;
        this.envEmbeddingProvider = envEmbeddingProvider;
        this.envEmbeddingBaseUrl = envEmbeddingBaseUrl;
        this.envEmbeddingApiKey = envEmbeddingApiKey;
        this.envEmbeddingModel = envEmbeddingModel;
        this.envEmbeddingDimension = envEmbeddingDimension;
    }

    public ModelMetadata chatMetadata() {
        return defaultConfig("chat")
                .map(config -> new ModelMetadata(toUuid(config.get("id")), string(config.get("provider")), string(config.get("model_name"))))
                .orElse(new ModelMetadata(null, envProvider, envDeepSeekModel));
    }

    public ModelInvocationResult answer(String systemPrompt,
                                        String userPrompt,
                                        String question,
                                        String mode,
                                        List<KnowledgeEvidence> evidences) {
        if (evidences.isEmpty()) {
            return invocationMetadata(ChatService.INSUFFICIENT_ANSWER);
        }
        Map<String, Object> config = defaultConfig("chat").orElseGet(this::environmentChatConfig);
        String content = invokeChatConfig(config, systemPrompt, userPrompt, false);
        return result(config, content);
    }

    public ModelInvocationResult analyzeRequirement(String systemPrompt,
                                                     String userPrompt,
                                                     String requirementDesc,
                                                     String moduleName,
                                                     List<KnowledgeEvidence> evidences) {
        if (evidences.isEmpty()) {
            return invocationMetadata("{\"feasibility_level\":\"D\",\"conclusion\":\"" + ChatService.INSUFFICIENT_ANSWER + "\"}");
        }
        Map<String, Object> config = defaultConfig("chat").orElseGet(this::environmentChatConfig);
        String content = invokeChatConfig(config, systemPrompt, userPrompt, true);
        return result(config, content);
    }

    public List<Double> embedText(String text) {
        Map<String, Object> config = defaultConfig("embedding").orElseGet(this::environmentEmbeddingConfig);
        EmbeddingModel embeddingModel = modelFactory.createEmbeddingModel(
                string(config.get("provider")),
                string(config.get("base_url")),
                string(config.get("api_key_value")),
                string(config.get("model_name")),
                intObject(config.get("dimension"))
        );
        try {
            float[] embedding = embeddingModel.embed(text == null ? "" : text);
            validateDimension(embedding.length, intValue(config.get("dimension"), envEmbeddingDimension));
            return Arrays.stream(toDoubleArray(embedding)).boxed().toList();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Spring AI Embedding 调用失败：" + safeMessage(ex));
        }
    }

    public String embeddingProvider() {
        return defaultConfig("embedding").map(config -> string(config.get("provider"))).orElse(envEmbeddingProvider);
    }

    public int embeddingDimension() {
        return defaultConfig("embedding")
                .map(config -> intValue(config.get("dimension"), envEmbeddingDimension))
                .orElse(envEmbeddingDimension);
    }

    public boolean realEmbedding() {
        return defaultConfig("embedding")
                .map(config -> supportedProvider(string(config.get("provider"))) && hasText(config.get("api_key_value")))
                .orElse(supportedProvider(envEmbeddingProvider) && hasText(envEmbeddingApiKey));
    }

    public Map<String, Object> testConfig(Map<String, Object> config) {
        String modelType = string(config.get("model_type"));
        if ("chat".equals(modelType)) {
            String content = invokeChatConfig(config, "你是连接测试助手，只返回一句简短中文。", "请回答：模型连接正常。", false);
            return Map.of("success", true, "message", "Spring AI 模型连接正常", "sample", content);
        }
        if ("embedding".equals(modelType)) {
            EmbeddingModel model = modelFactory.createEmbeddingModel(
                    string(config.get("provider")), string(config.get("base_url")), string(config.get("api_key_value")),
                    string(config.get("model_name")), intObject(config.get("dimension")));
            float[] vector = model.embed("水务模型配置测试");
            validateDimension(vector.length, intValue(config.get("dimension"), vector.length));
            return Map.of("success", true, "message", "Spring AI Embedding 连接正常", "dimension", vector.length);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "model_type 不支持");
    }

    private java.util.Optional<Map<String, Object>> defaultConfig(String modelType) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT *
                FROM ai_model_config
                WHERE deleted = FALSE
                  AND enabled = TRUE
                  AND default_config = TRUE
                  AND model_type = :model_type
                  AND provider <> 'mock'
                ORDER BY updated_at DESC
                LIMIT 1
                """, new MapSqlParameterSource("model_type", modelType), new ColumnMapRowMapper());
        return rows.stream().findFirst();
    }

    private String invokeChatConfig(Map<String, Object> config, String systemPrompt, String userPrompt, boolean structuredOutput) {
        try {
            ChatClient chatClient = modelFactory.createChatClient(
                    string(config.get("provider")),
                    string(config.get("base_url")),
                    string(config.get("api_key_value")),
                    string(config.get("model_name")),
                    intObject(config.get("max_tokens")),
                    decimal(config.get("temperature")),
                    structuredOutput
            );
            if (!structuredOutput) {
                String content = chatClient.prompt()
                        .system(nullToEmpty(systemPrompt))
                        .user(nullToEmpty(userPrompt))
                        .call()
                        .content();
                return requireContent(content);
            }

            BeanOutputConverter<RequirementModelOutput> converter = new BeanOutputConverter<>(RequirementModelOutput.class, objectMapper);
            RequirementModelOutput value = chatClient.prompt()
                    .system(nullToEmpty(systemPrompt))
                    .user(nullToEmpty(userPrompt) + "\n\n" + converter.getFormat())
                    .call()
                    .entity(converter);
            if (value == null) {
                throw new IllegalStateException("模型返回的结构化结果为空");
            }
            return objectMapper.writeValueAsString(value);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            // 模型异常只返回压缩后的错误信息，禁止把 API Key 或完整请求写入日志和响应。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Spring AI 模型调用失败：" + safeMessage(ex));
        }
    }

    private Map<String, Object> environmentChatConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("id", null);
        config.put("provider", envProvider);
        config.put("base_url", envDeepSeekBaseUrl);
        config.put("api_key_value", envDeepSeekApiKey);
        config.put("model_name", envDeepSeekModel);
        config.put("max_tokens", envDeepSeekMaxTokens);
        config.put("temperature", BigDecimal.valueOf(0.2));
        return config;
    }

    private Map<String, Object> environmentEmbeddingConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("provider", envEmbeddingProvider);
        config.put("base_url", envEmbeddingBaseUrl);
        config.put("api_key_value", envEmbeddingApiKey);
        config.put("model_name", envEmbeddingModel);
        config.put("dimension", envEmbeddingDimension);
        return config;
    }

    private ModelInvocationResult invocationMetadata(String content) {
        ModelMetadata metadata = chatMetadata();
        return new ModelInvocationResult(metadata.modelConfigId(), metadata.provider(), metadata.modelName(), content);
    }

    private ModelInvocationResult result(Map<String, Object> config, String content) {
        return new ModelInvocationResult(toUuid(config.get("id")), string(config.get("provider")), string(config.get("model_name")), content);
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("模型返回内容为空");
        }
        return content.trim();
    }

    private void validateDimension(int actual, int expected) {
        if (expected > 0 && actual != expected) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Embedding 维度不匹配，期望 " + expected + "，实际 " + actual);
        }
    }

    private double[] toDoubleArray(float[] values) {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }

    private boolean supportedProvider(String provider) {
        return "deepseek".equals(provider) || "openai".equals(provider) || "openai_compatible".equals(provider);
    }

    private boolean hasText(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
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
        return value == null ? null : ((Number) value).intValue();
    }

    private int intValue(Object value, int defaultValue) {
        return value == null ? defaultValue : ((Number) value).intValue();
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }
}
