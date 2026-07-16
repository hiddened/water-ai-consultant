package com.waterai.consultant.retrieval;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SpringAiDocumentMapper {

    public List<Document> toDocuments(List<KnowledgeEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return List.of();
        }
        return evidences.stream().map(this::toDocument).toList();
    }

    public Document toDocument(KnowledgeEvidence evidence) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source_type", evidence.sourceType());
        metadata.put("source_id", evidence.sourceId().toString());
        metadata.put("title", value(evidence.sourceTitle()));
        metadata.put("source_locator", value(evidence.sourceLocator()));
        metadata.put("module_name", value(evidence.moduleName()));
        metadata.put("score", evidence.score());
        if (evidence.documentId() != null) {
            metadata.put("document_id", evidence.documentId().toString());
        }
        if (evidence.chunkIndex() != null) {
            metadata.put("chunk_index", evidence.chunkIndex());
        }
        return Document.builder()
                .id(evidence.sourceId().toString())
                .text(value(evidence.content()))
                .metadata(metadata)
                .score(evidence.score().doubleValue())
                .build();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
