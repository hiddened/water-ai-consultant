package com.waterai.consultant.model;

import java.util.UUID;

public record ModelMetadata(
        UUID modelConfigId,
        String provider,
        String modelName
) {
}
