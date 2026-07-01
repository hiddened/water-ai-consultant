package com.waterai.consultant.requirement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.waterai.consultant.chat.RelatedItem;
import com.waterai.consultant.retrieval.AnswerReferenceDto;

import java.util.List;
import java.util.UUID;

public record RequirementCheckResponse(
        @JsonProperty("requirement_understanding")
        String requirementUnderstanding,
        @JsonProperty("feasibility_level")
        String feasibilityLevel,
        String conclusion,
        @JsonProperty("matched_capabilities")
        List<RelatedItem> matchedCapabilities,
        @JsonProperty("missing_capabilities")
        List<String> missingCapabilities,
        @JsonProperty("related_pages")
        List<RelatedItem> relatedPages,
        @JsonProperty("related_apis")
        List<RelatedItem> relatedApis,
        @JsonProperty("related_tables")
        List<RelatedItem> relatedTables,
        @JsonProperty("impact_modules")
        List<String> impactModules,
        @JsonProperty("risk_points")
        List<String> riskPoints,
        @JsonProperty("recommended_solution")
        String recommendedSolution,
        @JsonProperty("workload_level")
        String workloadLevel,
        List<AnswerReferenceDto> references,
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
