package com.waterai.consultant.retrieval;

import java.math.BigDecimal;
import java.util.UUID;

public record KnowledgeEvidence(
        String sourceType,
        UUID sourceId,
        String sourceTitle,
        String sourceLocator,
        String moduleName,
        String content,
        BigDecimal score,
        UUID documentId,
        String documentTitle,
        Integer chunkIndex,
        Integer pageNumber,
        String sectionTitle,
        String scoreType,
        String indexStatus
) {
    public KnowledgeEvidence(String sourceType,
                             UUID sourceId,
                             String sourceTitle,
                             String sourceLocator,
                             String moduleName,
                             String content,
                             BigDecimal score) {
        this(sourceType, sourceId, sourceTitle, sourceLocator, moduleName, content, score,
                null, null, null, null, null, "keyword_score", null);
    }

    public KnowledgeEvidence(String sourceType,
                             UUID sourceId,
                             String sourceTitle,
                             String sourceLocator,
                             String moduleName,
                             String content,
                             BigDecimal score,
                             UUID documentId,
                             String documentTitle,
                             Integer chunkIndex,
                             Integer pageNumber,
                             String sectionTitle) {
        this(sourceType, sourceId, sourceTitle, sourceLocator, moduleName, content, score,
                documentId, documentTitle, chunkIndex, pageNumber, sectionTitle, "keyword_score", null);
    }
}
