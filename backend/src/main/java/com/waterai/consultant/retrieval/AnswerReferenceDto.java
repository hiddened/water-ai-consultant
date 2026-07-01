package com.waterai.consultant.retrieval;

import java.math.BigDecimal;
import java.util.UUID;

public record AnswerReferenceDto(
        String sourceType,
        UUID sourceId,
        String title,
        String sourceTitle,
        String sourceLocator,
        UUID chunkId,
        Integer chunkIndex,
        UUID documentId,
        String documentTitle,
        String contentPreview,
        String content,
        String moduleName,
        String scoreType,
        String indexStatus,
        String quote,
        BigDecimal score
) {
    public static AnswerReferenceDto from(KnowledgeEvidence evidence) {
        String preview = preview(evidence.content(), 180);
        UUID chunkId = "DOCUMENT_CHUNK".equals(evidence.sourceType()) ? evidence.sourceId() : null;
        return new AnswerReferenceDto(
                evidence.sourceType(),
                evidence.sourceId(),
                evidence.sourceTitle(),
                evidence.sourceTitle(),
                evidence.sourceLocator(),
                chunkId,
                evidence.chunkIndex(),
                evidence.documentId(),
                evidence.documentTitle(),
                preview,
                evidence.content(),
                evidence.moduleName(),
                evidence.scoreType(),
                evidence.indexStatus(),
                preview,
                evidence.score()
        );
    }

    private static String preview(String value, int length) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > length ? normalized.substring(0, length) + "..." : normalized;
    }
}
