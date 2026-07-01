package com.waterai.consultant.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PromptTemplateTestRequest(
        String question,
        @JsonProperty("requirement_desc")
        String requirementDesc,
        @JsonProperty("project_name")
        String projectName,
        String mode,
        String references,
        String context,
        @JsonProperty("module_name")
        String moduleName
) {
}
