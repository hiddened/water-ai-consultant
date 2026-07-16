package com.waterai.consultant.vector;

import com.waterai.consultant.model.ModelRuntimeService;
import com.waterai.consultant.retrieval.DocumentChunkSearchService;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.vector.provider", havingValue = "pgvector", matchIfMissing = true)
public class PgVectorStore implements VectorStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ModelRuntimeService modelRuntimeService;
    private final DocumentChunkSearchService keywordSearchService;

    public PgVectorStore(NamedParameterJdbcTemplate jdbcTemplate,
                         ModelRuntimeService modelRuntimeService,
                         DocumentChunkSearchService keywordSearchService) {
        this.jdbcTemplate = jdbcTemplate;
        this.modelRuntimeService = modelRuntimeService;
        this.keywordSearchService = keywordSearchService;
    }

    @Override
    public VectorSearchResult searchWithMetadata(UUID projectId, String query, List<String> terms, int limit) {
        if (!modelRuntimeService.realEmbedding() || !vectorColumnAvailable()) {
            return new VectorSearchResult(keywordSearchService.keywordSearch(projectId, terms, limit),
                    "keyword", modelRuntimeService.embeddingProvider(), false, true);
        }

        boolean fallbackUsed = false;
        List<KnowledgeEvidence> vectorHits;
        try {
            vectorHits = vectorSearch(projectId, query, limit);
        } catch (RuntimeException ex) {
            fallbackUsed = true;
            return new VectorSearchResult(keywordSearchService.keywordSearch(projectId, terms, limit),
                    "fallback_keyword", modelRuntimeService.embeddingProvider(), true, true);
        }

        List<KnowledgeEvidence> finalHits = new ArrayList<>(vectorHits);
        if (finalHits.size() < limit) {
            fallbackUsed = true;
            finalHits.addAll(keywordSearchService.keywordSearch(projectId, terms, limit));
        }
        List<KnowledgeEvidence> merged = dedupe(finalHits).stream().limit(limit).toList();
        String strategy = fallbackUsed ? "hybrid" : "vector";
        return new VectorSearchResult(merged, strategy, modelRuntimeService.embeddingProvider(), true, fallbackUsed);
    }

    @Override
    public void add(List<Document> documents) {
        for (Document document : documents) {
            UUID chunkId = chunkId(document);
            try {
                updateEmbedding(chunkId, modelRuntimeService.embedText(document.getText()));
            } catch (RuntimeException ex) {
                updateIndexStatus(chunkId, "failed", ex.getMessage());
                throw ex;
            }
        }
    }

    @Override
    public void delete(List<String> ids) {
        for (String id : ids) {
            jdbcTemplate.update("""
                    UPDATE ai_document_chunk
                    SET embedding = NULL, embedding_model = NULL, index_status = 'pending',
                        index_error = NULL, last_indexed_at = NULL, updated_at = now()
                    WHERE id = :id
                    """, new MapSqlParameterSource("id", UUID.fromString(id)));
        }
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        UUID projectId = extractProjectId(filterExpression);
        if (projectId == null) {
            throw new IllegalArgumentException("删除向量必须提供 project_id 过滤条件");
        }
        jdbcTemplate.update("""
                UPDATE ai_document_chunk
                SET embedding = NULL, embedding_model = NULL, index_status = 'pending',
                    index_error = NULL, last_indexed_at = NULL, updated_at = now()
                WHERE project_id = :project_id AND deleted = FALSE
                """, new MapSqlParameterSource("project_id", projectId));
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        UUID projectId = extractProjectId(request.getFilterExpression());
        if (projectId == null) {
            // 多项目知识必须显式隔离，禁止标准 VectorStore 调用跨项目召回。
            throw new IllegalArgumentException("向量检索必须提供 project_id 过滤条件");
        }
        if (!modelRuntimeService.realEmbedding() || !vectorColumnAvailable()) {
            return List.of();
        }
        return vectorSearch(projectId, request.getQuery(), request.getTopK()).stream()
                .filter(evidence -> evidence.score().doubleValue() >= request.getSimilarityThreshold())
                .map(this::toSpringAiDocument)
                .toList();
    }

    public int indexDocument(UUID documentId) {
        List<Map<String, Object>> chunks = jdbcTemplate.queryForList("""
                SELECT id, content FROM ai_document_chunk
                WHERE document_id = :document_id AND deleted = FALSE
                ORDER BY COALESCE(chunk_index, chunk_no)
                """, new MapSqlParameterSource("document_id", documentId));
        int indexed = indexChunks(chunks);
        refreshDocumentIndexSummary(documentId);
        return indexed;
    }

    public int indexAll() {
        List<Map<String, Object>> documents = jdbcTemplate.queryForList("""
                SELECT id FROM ai_document
                WHERE deleted = FALSE AND enabled = TRUE AND parse_status = 'ready'
                ORDER BY updated_at DESC
                """, new MapSqlParameterSource());
        int total = 0;
        for (Map<String, Object> document : documents) {
            total += indexDocument(UUID.fromString(String.valueOf(document.get("id"))));
        }
        return total;
    }

    private int indexChunks(List<Map<String, Object>> chunks) {
        if (chunks.isEmpty()) {
            return 0;
        }
        if (!vectorColumnAvailable()) {
            markChunksFailed(chunks, "pgvector extension 或 embedding 列不可用");
            return 0;
        }

        int indexed = 0;
        for (Map<String, Object> chunk : chunks) {
            UUID chunkId = UUID.fromString(String.valueOf(chunk.get("id")));
            try {
                add(List.of(Document.builder()
                        .id(chunkId.toString())
                        .text(String.valueOf(chunk.get("content")))
                        .metadata("chunk_id", chunkId.toString())
                        .build()));
                indexed++;
            } catch (Exception ex) {
                updateIndexStatus(chunkId, "failed", ex.getMessage());
            }
        }
        return indexed;
    }

    private List<KnowledgeEvidence> vectorSearch(UUID projectId, String query, int limit) {
        List<Double> queryEmbedding = modelRuntimeService.embedText(query);
        String sql = """
                SELECT c.id, c.document_id, COALESCE(c.document_title, d.document_name) AS document_title,
                       d.module_name, c.source_locator, COALESCE(c.chunk_index, c.chunk_no) AS chunk_index,
                       COALESCE(c.page_number, c.page_no) AS page_number, c.section_title, c.index_status, c.content,
                       (c.embedding <=> CAST(:embedding AS vector)) AS distance
                FROM ai_document_chunk c
                JOIN ai_document d ON d.id = c.document_id
                WHERE c.project_id = :project_id
                  AND c.deleted = FALSE
                  AND d.deleted = FALSE
                  AND d.enabled = TRUE
                  AND d.parse_status = 'ready'
                  AND c.embedding IS NOT NULL
                ORDER BY c.embedding <=> CAST(:embedding AS vector)
                LIMIT :limit
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("embedding", vectorLiteral(queryEmbedding))
                .addValue("limit", limit), (rs, rowNum) -> {
            double distance = rs.getDouble("distance");
            BigDecimal score = BigDecimal.valueOf(1.0d - distance).setScale(6, RoundingMode.HALF_UP);
            String documentTitle = rs.getString("document_title");
            Integer chunkIndex = rs.getInt("chunk_index");
            Integer pageNumber = (Integer) rs.getObject("page_number");
            String sectionTitle = rs.getString("section_title");
            return new KnowledgeEvidence(
                    "DOCUMENT_CHUNK",
                    UUID.fromString(rs.getString("id")),
                    documentTitle + " #chunk-" + chunkIndex,
                    rs.getString("source_locator"),
                    rs.getString("module_name"),
                    decorate(documentTitle, chunkIndex, pageNumber, sectionTitle, rs.getString("content")),
                    score,
                    UUID.fromString(rs.getString("document_id")),
                    documentTitle,
                    chunkIndex,
                    pageNumber,
                    sectionTitle,
                    "vector_distance",
                    rs.getString("index_status")
            );
        });
    }

    private List<KnowledgeEvidence> dedupe(List<KnowledgeEvidence> evidences) {
        Map<UUID, KnowledgeEvidence> map = new LinkedHashMap<>();
        for (KnowledgeEvidence evidence : evidences) {
            KnowledgeEvidence existing = map.get(evidence.sourceId());
            if (existing == null || evidence.score().compareTo(existing.score()) > 0) {
                map.put(evidence.sourceId(), evidence);
            }
        }
        return map.values().stream()
                .sorted(Comparator.comparing(KnowledgeEvidence::score, Comparator.reverseOrder()))
                .toList();
    }

    private void updateEmbedding(UUID chunkId, List<Double> vector) {
        jdbcTemplate.update("""
                UPDATE ai_document_chunk
                SET embedding = CAST(:embedding AS vector),
                    embedding_model = :embedding_model,
                    index_status = 'indexed',
                    index_error = NULL,
                    last_indexed_at = now(),
                    updated_at = now()
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", chunkId)
                .addValue("embedding", vectorLiteral(vector))
                .addValue("embedding_model", modelRuntimeService.embeddingProvider()));
    }

    private void updateIndexStatus(UUID chunkId, String status, String error) {
        jdbcTemplate.update("""
                UPDATE ai_document_chunk
                SET index_status = :status, index_error = :error, last_indexed_at = CASE WHEN :status = 'indexed' THEN now() ELSE last_indexed_at END, updated_at = now()
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", chunkId)
                .addValue("status", status)
                .addValue("error", error));
    }

    private void markChunksFailed(List<Map<String, Object>> chunks, String message) {
        for (Map<String, Object> chunk : chunks) {
            updateIndexStatus(UUID.fromString(String.valueOf(chunk.get("id"))), "failed", message);
        }
    }

    private void refreshDocumentIndexSummary(UUID documentId) {
        jdbcTemplate.update("""
                UPDATE ai_document d
                SET chunk_count = stats.total_count,
                    index_status = CASE
                        WHEN stats.total_count = 0 THEN 'pending'
                        WHEN stats.failed_count > 0 THEN 'failed'
                        WHEN stats.indexed_count = stats.total_count THEN 'indexed'
                        ELSE 'pending'
                    END,
                    index_error = stats.last_error,
                    last_indexed_at = stats.last_indexed_at,
                    updated_at = now()
                FROM (
                    SELECT document_id,
                           COUNT(*)::int AS total_count,
                           COUNT(*) FILTER (WHERE index_status = 'indexed')::int AS indexed_count,
                           COUNT(*) FILTER (WHERE index_status = 'failed')::int AS failed_count,
                           MAX(index_error) FILTER (WHERE index_error IS NOT NULL) AS last_error,
                           MAX(last_indexed_at) AS last_indexed_at
                    FROM ai_document_chunk
                    WHERE document_id = :document_id AND deleted = FALSE
                    GROUP BY document_id
                ) stats
                WHERE d.id = stats.document_id
                """, new MapSqlParameterSource("document_id", documentId));
    }

    private boolean vectorColumnAvailable() {
        try {
            Boolean exists = jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'ai_document_chunk' AND column_name = 'embedding'
                    )
                    """, new MapSqlParameterSource(), Boolean.class);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            return false;
        }
    }

    private String vectorLiteral(List<Double> vector) {
        if (vector.size() != modelRuntimeService.embeddingDimension()) {
            throw new IllegalStateException("Embedding 维度不匹配，期望 " + modelRuntimeService.embeddingDimension() + "，实际 " + vector.size());
        }
        return "[" + String.join(",", vector.stream().map(value -> BigDecimal.valueOf(value).toPlainString()).toList()) + "]";
    }

    private String decorate(String title, int chunkIndex, Integer pageNumber, String sectionTitle, String content) {
        String page = pageNumber == null ? "" : " page=" + pageNumber;
        String section = sectionTitle == null || sectionTitle.isBlank() ? "" : " section=" + sectionTitle;
        return "document=" + title + " chunk=" + chunkIndex + page + section + "\n" + content;
    }

    private UUID chunkId(Document document) {
        Object metadataId = document.getMetadata().get("chunk_id");
        String value = metadataId == null ? document.getId() : String.valueOf(metadataId);
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Spring AI Document 缺少有效 chunk_id", ex);
        }
    }

    private Document toSpringAiDocument(KnowledgeEvidence evidence) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("project_scoped", true);
        metadata.put("source_type", evidence.sourceType());
        metadata.put("chunk_id", evidence.sourceId().toString());
        metadata.put("document_id", evidence.documentId() == null ? "" : evidence.documentId().toString());
        metadata.put("document_title", evidence.documentTitle());
        metadata.put("chunk_index", evidence.chunkIndex());
        metadata.put("module_name", evidence.moduleName());
        return Document.builder()
                .id(evidence.sourceId().toString())
                .text(evidence.content())
                .metadata(metadata)
                .score(evidence.score().doubleValue())
                .build();
    }

    private UUID extractProjectId(Filter.Expression expression) {
        if (expression == null) {
            return null;
        }
        if (expression.type() == Filter.ExpressionType.EQ
                && expression.left() instanceof Filter.Key key
                && "project_id".equals(key.key())
                && expression.right() instanceof Filter.Value value) {
            return UUID.fromString(String.valueOf(value.value()));
        }
        if (expression.type() == Filter.ExpressionType.AND) {
            UUID left = expression.left() instanceof Filter.Expression nested ? extractProjectId(nested) : null;
            if (left != null) {
                return left;
            }
            return expression.right() instanceof Filter.Expression nested ? extractProjectId(nested) : null;
        }
        return null;
    }
}
