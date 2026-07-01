package com.waterai.consultant.eval;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record EvalCaseRequest(
        @JsonProperty("project_id")
        UUID projectId,
        String question,
        @JsonProperty("expected_answer")
        String expectedAnswer,
        @JsonProperty("expected_sources")
        Object expectedSources,
        @JsonProperty("expected_mode")
        String expectedMode,
        @JsonProperty("expected_feasibility_level")
        String expectedFeasibilityLevel,
        @JsonProperty("expected_keywords")
        Object expectedKeywords,
        @JsonProperty("expected_source_titles")
        Object expectedSourceTitles,
        @JsonProperty("expected_source_types")
        Object expectedSourceTypes,
        @JsonProperty("expected_refusal")
        Boolean expectedRefusal,
        @JsonProperty("expected_answer_type")
        String expectedAnswerType,
        @JsonProperty("score_rules")
        Object scoreRules,
        Object tags,
        String difficulty,
        String remark,
        Boolean enabled
) {
}
