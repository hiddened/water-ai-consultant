package com.waterai.consultant.document;

public record DocumentReindexResponse(
        int documentCount,
        int chunkCount,
        String message
) {
}
