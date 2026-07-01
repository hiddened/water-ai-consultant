package com.waterai.consultant.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ChatRequest(
        @JsonProperty("project_id")
        UUID projectId,
        @NotBlank
        String mode,
        @NotBlank
        String question
) {
}

