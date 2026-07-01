package com.waterai.consultant.retrieval;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import com.waterai.consultant.vector.VectorStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeRetrievalService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final KeywordExtractor keywordExtractor;
    private final DocumentChunkSearchService chunkSearchService;
    private final VectorStore vectorStore;

    public KnowledgeRetrievalService(NamedParameterJdbcTemplate jdbcTemplate,
                                     KeywordExtractor keywordExtractor,
                                     DocumentChunkSearchService chunkSearchService,
                                     VectorStore vectorStore) {
        this.jdbcTemplate = jdbcTemplate;
        this.keywordExtractor = keywordExtractor;
        this.chunkSearchService = chunkSearchService;
        this.vectorStore = vectorStore;
    }

    public List<KnowledgeEvidence> retrieve(UUID projectId, String query, String mode, int limit) {
        List<String> terms = keywordExtractor.extract(query);
        if (terms.isEmpty()) {
            return List.of();
        }

        List<KnowledgeEvidence> candidates = new ArrayList<>();
        candidates.addAll(queryDocumentChunks(projectId, query, terms, limit));
        candidates.addAll(queryDocuments(projectId, terms));
        candidates.addAll(queryPages(projectId, terms));
        candidates.addAll(queryCapabilities(projectId, terms));
        candidates.addAll(queryApis(projectId, terms));
        candidates.addAll(queryDbTables(projectId, terms));
        candidates.addAll(queryRequirementCases(projectId, terms));

        return candidates.stream()
                .filter(evidence -> evidence.score().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator
                        .comparing((KnowledgeEvidence evidence) -> modeBoost(mode, evidence.sourceType())).reversed()
                        .thenComparing(KnowledgeEvidence::score, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    private List<KnowledgeEvidence> queryDocumentChunks(UUID projectId, String query, List<String> terms, int limit) {
        try {
            return vectorStore.search(projectId, query, terms, limit);
        } catch (RuntimeException ex) {
            // 向量层不是硬依赖；真实向量库异常时自动降级，保证问答仍可基于关键词运行。
            return chunkSearchService.keywordSearch(projectId, terms, limit);
        }
    }

    private List<KnowledgeEvidence> queryDocuments(UUID projectId, List<String> terms) {
        String sql = """
                SELECT id, document_name AS title, module_name, file_path AS locator,
                       concat_ws(' ', document_name, module_name, document_type, file_path, parse_status, parse_error) AS content
                FROM ai_document
                WHERE project_id = :project_id AND deleted = FALSE AND enabled = TRUE
                """;
        return querySource(projectId, sql, "DOCUMENT", terms);
    }

    private List<KnowledgeEvidence> queryPages(UUID projectId, List<String> terms) {
        String sql = """
                SELECT id, page_name AS title, module_name, route_path AS locator,
                       concat_ws(' ', page_name, module_name, route_path, operation_desc, business_rule, keywords) AS content
                FROM ai_page
                WHERE project_id = :project_id AND deleted = FALSE AND enabled = TRUE
                """;
        return querySource(projectId, sql, "PAGE", terms);
    }

    private List<KnowledgeEvidence> queryCapabilities(UUID projectId, List<String> terms) {
        String sql = """
                SELECT id, capability_name AS title, module_name, support_level AS locator,
                       concat_ws(' ', capability_name, module_name, support_level, description, limitation, keywords) AS content
                FROM ai_capability
                WHERE project_id = :project_id AND deleted = FALSE AND enabled = TRUE
                """;
        return querySource(projectId, sql, "CAPABILITY", terms);
    }

    private List<KnowledgeEvidence> queryApis(UUID projectId, List<String> terms) {
        String sql = """
                SELECT id, api_name AS title, module_name, concat(method, ' ', path) AS locator,
                       concat_ws(' ', api_name, module_name, method, path, request_desc, response_desc, auth_desc, status, keywords) AS content
                FROM ai_api
                WHERE project_id = :project_id AND deleted = FALSE AND enabled = TRUE
                """;
        return querySource(projectId, sql, "API", terms);
    }

    private List<KnowledgeEvidence> queryDbTables(UUID projectId, List<String> terms) {
        String sql = """
                SELECT id, table_name AS title, module_name, table_name AS locator,
                       concat_ws(' ', table_name, table_comment, module_name, field_desc::text, relation_desc, keywords) AS content
                FROM ai_db_table
                WHERE project_id = :project_id AND deleted = FALSE AND enabled = TRUE
                """;
        return querySource(projectId, sql, "DB_TABLE", terms);
    }

    private List<KnowledgeEvidence> queryRequirementCases(UUID projectId, List<String> terms) {
        String sql = """
                SELECT id, case_title AS title, module_name, feasibility_level AS locator,
                       concat_ws(' ', case_title, module_name, requirement_desc, solution_desc, feasibility_level, workload_level, risk_points, keywords) AS content
                FROM ai_requirement_case
                WHERE project_id = :project_id AND deleted = FALSE AND enabled = TRUE
                """;
        return querySource(projectId, sql, "REQUIREMENT_CASE", terms);
    }

    private List<KnowledgeEvidence> querySource(UUID projectId, String sql, String sourceType, List<String> terms) {
        MapSqlParameterSource params = new MapSqlParameterSource("project_id", projectId);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
                    String title = rs.getString("title");
                    String moduleName = rs.getString("module_name");
                    String locator = rs.getString("locator");
                    String content = rs.getString("content");
                    BigDecimal score = score(title, moduleName, locator, content, terms);
                    return new KnowledgeEvidence(
                            sourceType,
                            UUID.fromString(rs.getString("id")),
                            title,
                            locator,
                            moduleName,
                            content,
                            score
                    );
                }).stream()
                .filter(evidence -> evidence.score().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private BigDecimal score(String title, String moduleName, String locator, String content, List<String> terms) {
        String titleText = safe(title).toLowerCase(Locale.ROOT);
        String moduleText = safe(moduleName).toLowerCase(Locale.ROOT);
        String locatorText = safe(locator).toLowerCase(Locale.ROOT);
        String contentText = safe(content).toLowerCase(Locale.ROOT);

        double score = 0;
        for (String term : terms) {
            if (titleText.contains(term)) score += 4;
            if (moduleText.contains(term)) score += 2.5;
            if (locatorText.contains(term)) score += 2;
            if (contentText.contains(term)) score += 1;
        }
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    private int modeBoost(String mode, String sourceType) {
        Map<String, List<String>> priority = Map.of(
                "page_help", List.of("PAGE", "DOCUMENT_CHUNK", "CAPABILITY", "API", "DOCUMENT", "REQUIREMENT_CASE", "DB_TABLE"),
                "business_qa", List.of("DOCUMENT_CHUNK", "CAPABILITY", "PAGE", "DB_TABLE", "API", "REQUIREMENT_CASE", "DOCUMENT"),
                "requirement_check", List.of("CAPABILITY", "REQUIREMENT_CASE", "PAGE", "API", "DB_TABLE", "DOCUMENT_CHUNK", "DOCUMENT"),
                "doc_qa", List.of("DOCUMENT_CHUNK", "DOCUMENT", "PAGE", "CAPABILITY", "REQUIREMENT_CASE", "API", "DB_TABLE")
        );
        List<String> order = priority.getOrDefault(mode, priority.get("doc_qa"));
        int index = order.indexOf(sourceType);
        return index < 0 ? 0 : order.size() - index;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
