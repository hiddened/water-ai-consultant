package com.waterai.consultant.health;

import java.time.OffsetDateTime;

public record HealthResponse(
        String status,
        String service,
        OffsetDateTime time
) {
}

