package com.waterai.consultant.document;

public record DocumentChunk(
        int chunkIndex,
        Integer pageNumber,
        String sectionTitle,
        String content
) {
}
