package com.waterai.consultant.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeedbackReviewRequest(
        @JsonProperty("review_status")
        String reviewStatus,
        String reviewer,
        @JsonProperty("review_remark")
        String reviewRemark,
        @JsonProperty("target_knowledge_type")
        String targetKnowledgeType
) {
}
