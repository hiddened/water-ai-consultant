package com.waterai.consultant.vector;

import com.waterai.consultant.retrieval.KnowledgeEvidence;

import java.util.List;

public record VectorSearchResult(
        List<KnowledgeEvidence> evidences,
        String searchStrategy,
        String embeddingProvider,
        boolean vectorEnabled,
        boolean keywordFallbackUsed
) {
}
