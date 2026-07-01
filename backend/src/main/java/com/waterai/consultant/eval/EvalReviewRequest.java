package com.waterai.consultant.eval;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record EvalReviewRequest(
        Boolean passed,
        @JsonProperty("manual_passed")
        Boolean manualPassed,
        BigDecimal score,
        String remark
) {
}
