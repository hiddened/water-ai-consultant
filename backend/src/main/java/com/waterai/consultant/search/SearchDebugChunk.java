package com.waterai.consultant.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record SearchDebugChunk(
        @JsonProperty("chunk_id")
        UUID chunkId,
        @JsonProperty("document_id")
        UUID documentId,
        @JsonProperty("document_title")
        String documentTitle,
        @JsonProperty("chunk_index")
        Integer chunkIndex,
        BigDecimal score,
        @JsonProperty("score_type")
        String scoreType,
        @JsonProperty("content_preview")
        String contentPreview,
        String content,
        @JsonProperty("source_type")
        String sourceType,
        @JsonProperty("page_number")
        Integer pageNumber,
        @JsonProperty("section_title")
        String sectionTitle,
        @JsonProperty("index_status")
        String indexStatus,
        @JsonProperty("module_name")
        String moduleName,
        @JsonProperty("source_locator")
        String sourceLocator
) {
}
