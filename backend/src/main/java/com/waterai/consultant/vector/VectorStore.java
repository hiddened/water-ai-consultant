package com.waterai.consultant.vector;

import com.waterai.consultant.retrieval.KnowledgeEvidence;

import java.util.List;
import java.util.UUID;

public interface VectorStore {

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
