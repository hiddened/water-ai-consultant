package com.waterai.consultant.task;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record TaskResponse(
        @JsonProperty("task_id")
        UUID taskId,
        String status,
        String message
) {
}
