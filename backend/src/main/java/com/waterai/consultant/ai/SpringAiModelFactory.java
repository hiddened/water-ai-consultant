package com.waterai.consultant.ai;

import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Component
public class SpringAiModelFactory {

    private static final Set<String> OPENAI_COMPATIBLE_PROVIDERS = Set.of("deepseek", "openai_compatible", "openai");

    public ChatClient createChatClient(String provider,
                                       String baseUrl,
                                       String apiKey,
                                       String modelName,
                                       Integer maxTokens,
                                       BigDecimal temperature,
                                       boolean structuredOutput) {
        validateProvider(provider, "聊天模型");
        validateConnection(baseUrl, apiKey, modelName, "聊天模型");

        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder().model(modelName);
        if (maxTokens != null) {
            options.maxTokens(maxTokens);
        }
        if (temperature != null) {
            options.temperature(temperature.doubleValue());
        }
        if (structuredOutput) {
            options.responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build());
        }
        if ("deepseek".equals(provider)) {
            // DeepSeek 兼容 OpenAI 协议；关闭思考模式，避免结构化响应混入推理文本。
            options.extraBody(Map.of("thinking", Map.of("type", "disabled")));
        }

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .apiKey(apiKey)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options.build())
                .build();
        return ChatClient.create(model);
    }

    public EmbeddingModel createEmbeddingModel(String provider,
                                                String baseUrl,
                                                String apiKey,
                                                String modelName,
                                                Integer dimension) {
        validateProvider(provider, "Embedding 模型");
        validateConnection(baseUrl, apiKey, modelName, "Embedding 模型");

        OpenAiEmbeddingOptions.Builder options = OpenAiEmbeddingOptions.builder().model(modelName);
        if (dimension != null) {
            options.dimensions(dimension);
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .apiKey(apiKey)
                .build();
        return new OpenAiEmbeddingModel(api, org.springframework.ai.document.MetadataMode.EMBED, options.build());
    }

    private void validateProvider(String provider, String modelType) {
        if (!OPENAI_COMPATIBLE_PROVIDERS.contains(provider)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, modelType + " provider 不支持 Spring AI OpenAI 兼容协议：" + provider);
        }
    }

    private void validateConnection(String baseUrl, String apiKey, String modelName, String modelType) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, modelType + " Base URL 未配置");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, modelType + " API Key 未配置");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, modelType + "名称未配置");
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
