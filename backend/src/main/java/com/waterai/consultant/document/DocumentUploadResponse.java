package com.waterai.consultant.document;

import java.util.Map;
import java.util.UUID;

public record DocumentUploadResponse(
        Map<String, Object> document,
        int chunkCount,
        UUID taskId
) {
}
