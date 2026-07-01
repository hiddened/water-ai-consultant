package com.waterai.consultant.vector;

import com.waterai.consultant.retrieval.DocumentChunkSearchService;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.vector.provider", havingValue = "mock")
public class MockVectorStore implements VectorStore {

    private final DocumentChunkSearchService chunkSearchService;

    public MockVectorStore(DocumentChunkSearchService chunkSearchService) {
        this.chunkSearchService = chunkSearchService;
    }

    @Override
    public VectorSearchResult searchWithMetadata(UUID projectId, String query, List<String> terms, int limit) {
        // 第一版没有真实 embedding，先用相同接口包装 chunk 关键词检索，后续可无痛替换 pgvector。
        return new VectorSearchResult(chunkSearchService.keywordSearch(projectId, terms, limit),
                "fallback_keyword", "mock", false, true);
    }
}
