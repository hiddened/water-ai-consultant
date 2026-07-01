package com.waterai.consultant.model;

import java.util.UUID;

public record ModelInvocationResult(
        UUID modelConfigId,
        String provider,
        String modelName,
        String content
) {
}
