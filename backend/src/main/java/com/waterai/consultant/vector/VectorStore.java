package com.waterai.consultant.vector;

import com.waterai.consultant.retrieval.KnowledgeEvidence;

import java.util.List;
import java.util.UUID;

/**
 * 在 Spring AI VectorStore 标准能力上扩展项目级检索和索引任务信息。
 */
public interface VectorStore extends org.springframework.ai.vectorstore.VectorStore {

    VectorSearchResult searchWithMetadata(UUID projectId, String query, List<String> terms, int limit);

    default List<KnowledgeEvidence> search(UUID projectId, String query, List<String> terms, int limit) {
        return searchWithMetadata(projectId, query, terms, limit).evidences();
    }

    default int indexDocument(UUID documentId) {
        return 0;
    }

    default int indexAll() {
        return 0;
    }
}
