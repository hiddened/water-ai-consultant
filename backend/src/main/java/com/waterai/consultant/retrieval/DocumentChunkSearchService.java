package com.waterai.consultant.retrieval;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentChunkSearchService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DocumentChunkSearchService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KnowledgeEvidence> keywordSearch(UUID projectId, List<String> terms, int limit) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT c.id,
                       c.document_id,
                       COALESCE(c.document_title, d.document_name) AS title,
                       d.module_name,
                       c.source_locator,
                       COALESCE(c.chunk_index, c.chunk_no) AS chunk_index,
                       COALESCE(c.page_number, c.page_no) AS page_number,
                       c.section_title,
                       c.index_status,
                       c.content
                FROM ai_document_chunk c
                JOIN ai_document d ON d.id = c.document_id
                WHERE c.project_id = :project_id
                  AND c.deleted = FALSE
                  AND d.deleted = FALSE
                  AND d.enabled = TRUE
                  AND d.parse_status = 'ready'
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("project_id", projectId), (rs, rowNum) -> {
                    String title = rs.getString("title");
                    String moduleName = rs.getString("module_name");
                    Integer pageNumber = (Integer) rs.getObject("page_number");
                    int chunkIndex = rs.getInt("chunk_index");
                    String sectionTitle = rs.getString("section_title");
                    String locator = rs.getString("source_locator");
                    String indexStatus = rs.getString("index_status");
                    String content = rs.getString("content");
                    String decoratedContent = decorate(title, chunkIndex, pageNumber, sectionTitle, content);
                    return new KnowledgeEvidence(
                            "DOCUMENT_CHUNK",
                            UUID.fromString(rs.getString("id")),
                            title + " #chunk-" + chunkIndex,
                            locator,
                            moduleName,
                            decoratedContent,
                            score(title, moduleName, locator, sectionTitle, content, terms),
                            UUID.fromString(rs.getString("document_id")),
                            title,
                            chunkIndex,
                            pageNumber,
                            sectionTitle,
                            "keyword_score",
                            indexStatus
                    );
                }).stream()
                .filter(evidence -> evidence.score().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(KnowledgeEvidence::score, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    private String decorate(String title, int chunkIndex, Integer pageNumber, String sectionTitle, String content) {
        String page = pageNumber == null ? "" : " page=" + pageNumber;
        String section = sectionTitle == null || sectionTitle.isBlank() ? "" : " section=" + sectionTitle;
        return "document=" + title + " chunk=" + chunkIndex + page + section + "\n" + content;
    }

    private BigDecimal score(String title, String moduleName, String locator, String sectionTitle, String content, List<String> terms) {
        String titleText = safe(title).toLowerCase(Locale.ROOT);
        String moduleText = safe(moduleName).toLowerCase(Locale.ROOT);
        String locatorText = safe(locator).toLowerCase(Locale.ROOT);
        String sectionText = safe(sectionTitle).toLowerCase(Locale.ROOT);
        String contentText = safe(content).toLowerCase(Locale.ROOT);

        double score = 0;
        for (String term : terms) {
            if (titleText.contains(term)) score += 5;
            if (sectionText.contains(term)) score += 4;
            if (moduleText.contains(term)) score += 2.5;
            if (locatorText.contains(term)) score += 2;
            if (contentText.contains(term)) score += 1.4;
        }
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
