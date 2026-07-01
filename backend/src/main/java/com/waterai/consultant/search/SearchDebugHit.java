package com.waterai.consultant.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record SearchDebugHit(
        @JsonProperty("source_type")
        String sourceType,
        @JsonProperty("source_id")
        UUID sourceId,
        String title,
        BigDecimal score,
        @JsonProperty("content_preview")
        String contentPreview,
        String content,
        @JsonProperty("source_locator")
        String sourceLocator,
        @JsonProperty("project_id")
        UUID projectId,
        @JsonProperty("project_name")
        String projectName,
        @JsonProperty("module_name")
        String moduleName
) {
}
