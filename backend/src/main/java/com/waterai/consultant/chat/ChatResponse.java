package com.waterai.consultant.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.waterai.consultant.retrieval.AnswerReferenceDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ChatResponse(
        String answer,
        List<AnswerReferenceDto> references,
        @JsonProperty("related_pages")
        List<RelatedItem> relatedPages,
        @JsonProperty("related_capabilities")
        List<RelatedItem> relatedCapabilities,
        @JsonProperty("related_apis")
        List<RelatedItem> relatedApis,
        BigDecimal confidence,
        @JsonProperty("trace_id")
        String traceId,
        @JsonProperty("session_id")
        UUID sessionId,
        @JsonProperty("message_id")
        UUID messageId,
        @JsonProperty("llm_provider")
        String llmProvider,
        @JsonProperty("model_provider")
        String modelProvider,
        @JsonProperty("model_name")
        String modelName,
        @JsonProperty("prompt_template_id")
        UUID promptTemplateId,
        @JsonProperty("prompt_template_name")
        String promptTemplateName,
        @JsonProperty("search_strategy")
        String searchStrategy,
        @JsonProperty("insufficient_answer")
        boolean insufficientAnswer
) {
}
