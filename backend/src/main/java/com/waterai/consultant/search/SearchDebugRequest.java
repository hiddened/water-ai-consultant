package com.waterai.consultant.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SearchDebugRequest(
        @JsonProperty("project_id")
        UUID projectId,
        @NotBlank
        String question,
        String mode,
        @JsonProperty("top_k")
        Integer topK
) {
}
