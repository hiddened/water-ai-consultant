package com.waterai.consultant.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record FeedbackRequest(
        @JsonProperty("project_id")
        UUID projectId,
        @JsonProperty("session_id")
        UUID sessionId,
        @JsonProperty("message_id")
        UUID messageId,
        @JsonProperty("feedback_type")
        String feedbackType,
        String remark,
        @JsonProperty("corrected_answer")
        String correctedAnswer,
        @JsonProperty("expected_sources")
        List<Object> expectedSources,
        @JsonProperty("convert_to_knowledge")
        Boolean convertToKnowledge,
        @JsonProperty("target_knowledge_type")
        String targetKnowledgeType
) {
}
