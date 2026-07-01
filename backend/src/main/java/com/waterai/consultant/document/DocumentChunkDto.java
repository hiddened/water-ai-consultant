package com.waterai.consultant.document;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentChunkDto(
        UUID id,
        UUID documentId,
        String documentTitle,
        int chunkIndex,
        Integer pageNumber,
        String sectionTitle,
        String content,
        String sourceLocator,
        String indexStatus,
        String indexError,
        OffsetDateTime createdAt
) {
}
