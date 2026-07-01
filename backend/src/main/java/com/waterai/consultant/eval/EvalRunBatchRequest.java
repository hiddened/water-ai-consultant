package com.waterai.consultant.eval;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record EvalRunBatchRequest(
        @JsonProperty("project_id")
        UUID projectId,
        @JsonProperty("run_name")
        String runName,
        String tags,
        @JsonProperty("expected_mode")
        String expectedMode
) {
}
