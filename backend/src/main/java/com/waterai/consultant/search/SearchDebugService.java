package com.waterai.consultant.search;

import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import com.waterai.consultant.retrieval.DocumentChunkSearchService;
import com.waterai.consultant.retrieval.KeywordExtractor;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import com.waterai.consultant.vector.VectorStore;
import com.waterai.consultant.vector.VectorSearchResult;
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
public class SearchDebugService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final KeywordExtractor keywordExtractor;
    private final VectorStore vectorStore;
    private final DocumentChunkSearchService chunkSearchService;

    public SearchDebugService(NamedParameterJdbcTemplate jdbcTemplate,
                              KeywordExtractor keywordExtractor,
                              VectorStore vectorStore,
                              DocumentChunkSearchService chunkSearchService) {
        this.jdbcTemplate = jdbcTemplate;
        this.keywordExtractor = keywordExtractor;
        this.vectorStore = vectorStore;
        this.chunkSearchService = chunkSearchService;
    }

    public SearchDebugResponse debug(SearchDebugRequest request) {
        if (request.projectId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "project_id 不能为空");
        }
        int limit = request.topK() == null || request.topK() <= 0 ? 10 : Math.min(request.topK(), 50);
        List<String> terms = keywordExtractor.extract(request.question());
        if (terms.isEmpty()) {
            return new SearchDebugResponse(request.question(), "empty_terms", "none", false, false, 0,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        VectorSearchResult chunkSearch = queryChunks(request.projectId(), request.question(), terms, limit);
        List<SearchDebugChunk> chunks = toChunks(chunkSearch.evidences());
        List<SearchDebugHit> pages = querySource(request.projectId(), terms, "PAGE", limit, """
                SELECT p.id, p.page_name AS title, p.module_name, p.route_path AS locator,
                       concat_ws(' ', p.page_name, p.module_name, p.route_path, p.operation_desc, p.business_rule, p.keywords) AS content,
                       pr.project_name
                FROM ai_page p JOIN ai_project pr ON pr.id = p.project_id
                WHERE p.project_id = :project_id AND p.deleted = FALSE AND p.enabled = TRUE
                """);
        List<SearchDebugHit> capabilities = querySource(request.projectId(), terms, "CAPABILITY", limit, """
                SELECT c.id, c.capability_name AS title, c.module_name, c.support_level AS locator,
                       concat_ws(' ', c.capability_name, c.module_name, c.support_level, c.description, c.limitation, c.keywords) AS content,
                       pr.project_name
                FROM ai_capability c JOIN ai_project pr ON pr.id = c.project_id
                WHERE c.project_id = :project_id AND c.deleted = FALSE AND c.enabled = TRUE
                """);
        List<SearchDebugHit> apis = querySource(request.projectId(), terms, "API", limit, """
                SELECT a.id, a.api_name AS title, a.module_name, concat(a.method, ' ', a.path) AS locator,
                       concat_ws(' ', a.api_name, a.module_name, a.method, a.path, a.request_desc, a.response_desc, a.auth_desc, a.status, a.keywords) AS content,
                       pr.project_name
                FROM ai_api a JOIN ai_project pr ON pr.id = a.project_id
                WHERE a.project_id = :project_id AND a.deleted = FALSE AND a.enabled = TRUE
                """);
        List<SearchDebugHit> tables = querySource(request.projectId(), terms, "DB_TABLE", limit, """
                SELECT t.id, t.table_name AS title, t.module_name, t.table_name AS locator,
                       concat_ws(' ', t.table_name, t.table_comment, t.module_name, t.field_desc::text, t.relation_desc, t.keywords) AS content,
                       pr.project_name
                FROM ai_db_table t JOIN ai_project pr ON pr.id = t.project_id
                WHERE t.project_id = :project_id AND t.deleted = FALSE AND t.enabled = TRUE
                """);
        List<SearchDebugHit> requirementCases = querySource(request.projectId(), terms, "REQUIREMENT_CASE", limit, """
                SELECT r.id, r.case_title AS title, r.module_name, r.feasibility_level AS locator,
                       concat_ws(' ', r.case_title, r.module_name, r.requirement_desc, r.solution_desc, r.feasibility_level, r.workload_level, r.risk_points, r.keywords) AS content,
                       pr.project_name
                FROM ai_requirement_case r JOIN ai_project pr ON pr.id = r.project_id
                WHERE r.project_id = :project_id AND r.deleted = FALSE AND r.enabled = TRUE
                """);
        int total = chunks.size() + pages.size() + capabilities.size() + apis.size() + tables.size() + requirementCases.size();
        return new SearchDebugResponse(request.question(), chunkSearch.searchStrategy(), chunkSearch.embeddingProvider(),
                chunkSearch.vectorEnabled(), chunkSearch.keywordFallbackUsed(), total,
                chunks, pages, capabilities, apis, tables, requirementCases);
    }

    private VectorSearchResult queryChunks(UUID projectId, String query, List<String> terms, int limit) {
        try {
            return vectorStore.searchWithMetadata(projectId, query, terms, limit);
        } catch (RuntimeException ex) {
            // 调试接口必须稳定可用，向量层失败时复用 chunk 关键词检索结果。
            return new VectorSearchResult(chunkSearchService.keywordSearch(projectId, terms, limit),
                    "fallback_keyword", "unknown", false, true);
        }
    }

    private List<SearchDebugChunk> toChunks(List<KnowledgeEvidence> evidences) {
        return evidences.stream()
                .map(evidence -> new SearchDebugChunk(
                        evidence.sourceId(),
                        evidence.documentId(),
                        evidence.documentTitle(),
                        evidence.chunkIndex(),
                        evidence.score(),
                        evidence.scoreType(),
                        preview(evidence.content()),
                        evidence.content(),
                        evidence.sourceType(),
                        evidence.pageNumber(),
                        evidence.sectionTitle(),
                        evidence.indexStatus(),
                        evidence.moduleName(),
                        evidence.sourceLocator()
                ))
                .toList();
    }

    private List<SearchDebugHit> querySource(UUID projectId, List<String> terms, String sourceType, int limit, String sql) {
        return jdbcTemplate.query(sql, new MapSqlParameterSource("project_id", projectId), (rs, rowNum) -> {
                    String title = rs.getString("title");
                    String moduleName = rs.getString("module_name");
                    String locator = rs.getString("locator");
                    String content = rs.getString("content");
                    return new SearchDebugHit(
                            sourceType,
                            UUID.fromString(rs.getString("id")),
                            title,
                            score(title, moduleName, locator, content, terms),
                            preview(content),
                            content,
                            locator,
                            projectId,
                            rs.getString("project_name"),
                            moduleName
                    );
                }).stream()
                .filter(hit -> hit.score().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(SearchDebugHit::score, Comparator.reverseOrder()))
                .limit(limit)
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

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 180 ? normalized.substring(0, 180) + "..." : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
