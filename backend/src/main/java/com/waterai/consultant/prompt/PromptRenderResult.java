package com.waterai.consultant.prompt;

import java.util.UUID;

public record PromptRenderResult(
        UUID templateId,
        String templateName,
        String systemPrompt,
        String userPrompt,
        String outputFormat
) {
}
