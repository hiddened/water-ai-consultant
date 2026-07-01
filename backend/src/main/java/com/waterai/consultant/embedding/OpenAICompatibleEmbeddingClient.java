package com.waterai.consultant.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "openai-compatible")
public class OpenAICompatibleEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int dimension;

    public OpenAICompatibleEmbeddingClient(ObjectMapper objectMapper,
                                           @Value("${app.embedding.base-url}") String baseUrl,
                                           @Value("${app.embedding.api-key}") String apiKey,
                                           @Value("${app.embedding.model}") String model,
                                           @Value("${app.embedding.dimension}") int dimension) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Embedding API Key 未配置");
        }
        this.objectMapper = objectMapper;
        this.model = model;
        this.dimension = dimension;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<Double> embedText(String text) {
        return embedBatch(List.of(text)).getFirst();
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", texts);
        try {
            String response = restClient.post()
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
                if (vector.size() != dimension) {
                    throw new IllegalStateException("Embedding 维度不匹配，期望 " + dimension + "，实际 " + vector.size());
                }
                result.add(vector);
            }
            return result;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Embedding 调用失败：" + ex.getMessage());
        }
    }

    @Override
    public String provider() {
        return "openai-compatible";
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public boolean realEmbedding() {
        return true;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Embedding Base URL 未配置");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
