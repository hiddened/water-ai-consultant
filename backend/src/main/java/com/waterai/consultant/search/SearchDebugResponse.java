package com.waterai.consultant.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SearchDebugResponse(
        String query,
        @JsonProperty("search_strategy")
        String searchStrategy,
        @JsonProperty("embedding_provider")
        String embeddingProvider,
        @JsonProperty("vector_enabled")
        boolean vectorEnabled,
        @JsonProperty("keyword_fallback_used")
        boolean keywordFallbackUsed,
        @JsonProperty("total_hits")
        int totalHits,
        List<SearchDebugChunk> chunks,
        @JsonProperty("related_pages")
        List<SearchDebugHit> relatedPages,
        @JsonProperty("related_capabilities")
        List<SearchDebugHit> relatedCapabilities,
        @JsonProperty("related_apis")
        List<SearchDebugHit> relatedApis,
        @JsonProperty("related_tables")
        List<SearchDebugHit> relatedTables,
        @JsonProperty("requirement_cases")
        List<SearchDebugHit> requirementCases
) {
}
