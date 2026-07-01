package com.waterai.consultant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ModelConfigRequest(
        @JsonProperty("config_name")
        String configName,
        String provider,
        @JsonProperty("model_type")
        String modelType,
        @JsonProperty("base_url")
        String baseUrl,
        @JsonProperty("api_key")
        String apiKey,
        @JsonProperty("model_name")
        String modelName,
        Integer dimension,
        BigDecimal temperature,
        @JsonProperty("max_tokens")
        Integer maxTokens,
        Boolean enabled,
        @JsonProperty("default_config")
        Boolean defaultConfig,
        String remark
) {
}
