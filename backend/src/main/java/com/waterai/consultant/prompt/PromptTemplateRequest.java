package com.waterai.consultant.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PromptTemplateRequest(
        @JsonProperty("template_name")
        String templateName,
        @JsonProperty("template_type")
        String templateType,
        String mode,
        @JsonProperty("system_prompt")
        String systemPrompt,
        @JsonProperty("user_prompt_template")
        String userPromptTemplate,
        @JsonProperty("output_format")
        String outputFormat,
        Boolean enabled,
        @JsonProperty("default_template")
        Boolean defaultTemplate,
        String remark
) {
}
