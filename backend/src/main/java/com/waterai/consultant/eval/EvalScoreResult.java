package com.waterai.consultant.eval;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record EvalScoreResult(
        BigDecimal autoScore,
        BigDecimal keywordScore,
        BigDecimal sourceScore,
        BigDecimal refusalScore,
        BigDecimal feasibilityScore,
        int referenceCount,
        List<String> matchedKeywords,
        List<String> matchedSources,
        List<String> missingKeywords,
        List<String> missingSources,
        boolean autoPassed,
        Map<String, Object> scoreDetail
) {
}
