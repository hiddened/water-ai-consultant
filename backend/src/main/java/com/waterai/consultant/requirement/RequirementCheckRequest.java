package com.waterai.consultant.requirement;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record RequirementCheckRequest(
        @JsonProperty("project_id")
        UUID projectId,
        @JsonProperty("requirement_desc")
        @NotBlank
        String requirementDesc,
        @JsonProperty("module_name")
        String moduleName
) {
}

