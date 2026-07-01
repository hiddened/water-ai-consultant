package com.waterai.consultant.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterai.consultant.chat.ChatRequest;
import com.waterai.consultant.chat.ChatResponse;
import com.waterai.consultant.chat.ChatService;
import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import com.waterai.consultant.requirement.RequirementCheckRequest;
import com.waterai.consultant.requirement.RequirementCheckResponse;
import com.waterai.consultant.requirement.RequirementCheckService;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EvalService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ChatService chatService;
    private final RequirementCheckService requirementCheckService;
    private final EvalScoringService scoringService;
    private final ObjectMapper objectMapper;

    public EvalService(NamedParameterJdbcTemplate jdbcTemplate,
                       ChatService chatService,
                       RequirementCheckService requirementCheckService,
                       EvalScoringService scoringService,
                       ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.chatService = chatService;
        this.requirementCheckService = requirementCheckService;
        this.scoringService = scoringService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> listCases(UUID projectId) {
        String sql = """
                SELECT c.*, p.project_name,
                       (SELECT row_to_json(r) FROM ai_eval_result r WHERE r.eval_case_id = c.id ORDER BY r.created_at DESC LIMIT 1) AS latest_result
                FROM ai_eval_case c
                JOIN ai_project p ON p.id = c.project_id
                WHERE (CAST(:project_id AS uuid) IS NULL OR c.project_id = CAST(:project_id AS uuid))
                ORDER BY c.updated_at DESC, c.created_at DESC
                """;
        return normalizeRows(jdbcTemplate.query(sql, new MapSqlParameterSource("project_id", projectId), new ColumnMapRowMapper()));
    }

    public Map<String, Object> createCase(EvalCaseRequest request) {
        if (request.projectId() == null || request.question() == null || request.question().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "project_id 和 question 不能为空");
        }
        String sql = """
                INSERT INTO ai_eval_case(
                    project_id, question, expected_answer, expected_sources, expected_mode, expected_feasibility_level,
                    expected_keywords, expected_source_titles, expected_source_types, expected_refusal, expected_answer_type,
                    score_rules, tags, difficulty, remark, enabled
                )
                VALUES (
                    :project_id, :question, :expected_answer, CAST(:expected_sources AS jsonb), :expected_mode, :expected_feasibility_level,
                    CAST(:expected_keywords AS jsonb), CAST(:expected_source_titles AS jsonb), CAST(:expected_source_types AS jsonb),
                    :expected_refusal, :expected_answer_type, CAST(:score_rules AS jsonb), CAST(:tags AS jsonb),
                    :difficulty, :remark, :enabled
                )
                RETURNING *
                """;
        return normalizeRow(jdbcTemplate.queryForObject(sql, params(request), new ColumnMapRowMapper()));
    }

    public Map<String, Object> updateCase(UUID id, EvalCaseRequest request) {
        String sql = """
                UPDATE ai_eval_case
                SET project_id = :project_id,
                    question = :question,
                    expected_answer = :expected_answer,
                    expected_sources = CAST(:expected_sources AS jsonb),
                    expected_mode = :expected_mode,
                    expected_feasibility_level = :expected_feasibility_level,
                    expected_keywords = CAST(:expected_keywords AS jsonb),
                    expected_source_titles = CAST(:expected_source_titles AS jsonb),
                    expected_source_types = CAST(:expected_source_types AS jsonb),
                    expected_refusal = :expected_refusal,
                    expected_answer_type = :expected_answer_type,
                    score_rules = CAST(:score_rules AS jsonb),
                    tags = CAST(:tags AS jsonb),
                    difficulty = :difficulty,
                    remark = :remark,
                    enabled = :enabled,
                    updated_at = now()
                WHERE id = :id
                RETURNING *
                """;
        MapSqlParameterSource params = params(request).addValue("id", id);
        return normalizeRow(jdbcTemplate.queryForObject(sql, params, new ColumnMapRowMapper()));
    }

    public void deleteCase(UUID id) {
        jdbcTemplate.update("DELETE FROM ai_eval_result WHERE eval_case_id = :id", new MapSqlParameterSource("id", id));
        int affected = jdbcTemplate.update("DELETE FROM ai_eval_case WHERE id = :id", new MapSqlParameterSource("id", id));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评测用例不存在");
        }
    }

    public Map<String, Object> runCase(UUID id) {
        return runCase(id, null);
    }

    public Map<String, Object> runBatch(EvalRunBatchRequest request) {
        EvalRunBatchRequest safeRequest = request == null ? new EvalRunBatchRequest(null, null, null, null) : request;
        List<Map<String, Object>> cases = queryEnabledCases(safeRequest.projectId(), safeRequest.tags(), safeRequest.expectedMode());
        UUID runId = createRun(safeRequest, cases.size());
        updateRunStatus(runId, "running", null, null);
        for (Map<String, Object> row : cases) {
            UUID caseId = UUID.fromString(String.valueOf(row.get("id")));
            runCase(caseId, runId);
        }
        updateRunStats(runId, "success");
        return getRun(runId);
    }

    public Map<String, Object> reviewResult(UUID id, EvalReviewRequest request) {
        Boolean manualPassed = request.manualPassed() != null ? request.manualPassed() : request.passed();
        String sql = """
                UPDATE ai_eval_result
                SET passed = :passed,
                    manual_passed = :manual_passed,
                    score = :score,
                    remark = :remark
                WHERE id = :id
                RETURNING *
                """;
        return normalizeRow(jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("passed", manualPassed)
                .addValue("manual_passed", manualPassed)
                .addValue("score", request.score())
                .addValue("remark", request.remark()), new ColumnMapRowMapper()));
    }

    public List<Map<String, Object>> listRuns(UUID projectId, String status) {
        String sql = """
                SELECT r.*, p.project_name
                FROM ai_eval_run r
                LEFT JOIN ai_project p ON p.id = r.project_id
                WHERE (CAST(:project_id AS uuid) IS NULL OR r.project_id = CAST(:project_id AS uuid))
                  AND (CAST(:status AS text) IS NULL OR r.status = CAST(:status AS text))
                ORDER BY r.created_at DESC
                """;
        return normalizeRows(jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("status", blankToNull(status)), new ColumnMapRowMapper()));
    }

    public Map<String, Object> getRun(UUID id) {
        return normalizeRow(jdbcTemplate.query("""
                        SELECT r.*, p.project_name
                        FROM ai_eval_run r
                        LEFT JOIN ai_project p ON p.id = r.project_id
                        WHERE r.id = :id
                        """, new MapSqlParameterSource("id", id), new ColumnMapRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评测运行不存在")));
    }

    public List<Map<String, Object>> listRunResults(UUID runId) {
        String sql = """
                SELECT r.*, c.question, c.expected_mode, c.expected_feasibility_level, c.expected_keywords,
                       c.expected_source_titles, c.expected_source_types, c.expected_refusal, c.tags, c.difficulty
                FROM ai_eval_result r
                JOIN ai_eval_case c ON c.id = r.eval_case_id
                WHERE r.run_id = :run_id
                ORDER BY r.created_at
                """;
        return normalizeRows(jdbcTemplate.query(sql, new MapSqlParameterSource("run_id", runId), new ColumnMapRowMapper()));
    }

    public Map<String, Object> runSummary(UUID runId) {
        String sql = """
                SELECT
                    COUNT(*) AS total_results,
                    COUNT(*) FILTER (WHERE auto_passed = TRUE) AS auto_passed_count,
                    COUNT(*) FILTER (WHERE auto_passed = FALSE OR error_message IS NOT NULL) AS failed_count,
                    ROUND(COALESCE(AVG(auto_score), 0), 2) AS average_score,
                    ROUND(CASE WHEN COUNT(*) = 0 THEN 0 ELSE COUNT(*) FILTER (WHERE auto_passed = TRUE) * 100.0 / COUNT(*) END, 2) AS pass_rate,
                    COALESCE(jsonb_agg(to_jsonb(t) ORDER BY t.auto_score ASC) FILTER (WHERE t.auto_score < 70 OR t.error_message IS NOT NULL), '[]'::jsonb) AS low_score_cases
                FROM (
                    SELECT r.id, r.eval_case_id, c.question, r.actual_answer, r.auto_score, r.keyword_score, r.source_score,
                           r.refusal_score, r.feasibility_score, r.auto_passed, r.error_message, r.missing_keywords, r.missing_sources
                    FROM ai_eval_result r
                    JOIN ai_eval_case c ON c.id = r.eval_case_id
                    WHERE r.run_id = :run_id
                ) t
                """;
        return normalizeRow(jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("run_id", runId), new ColumnMapRowMapper()));
    }

    public Map<String, Object> rerunFailed(UUID runId) {
        Map<String, Object> oldRun = getRun(runId);
        List<Map<String, Object>> failedCases = jdbcTemplate.query("""
                SELECT DISTINCT r.eval_case_id AS id
                FROM ai_eval_result r
                WHERE r.run_id = :run_id AND (r.auto_passed = FALSE OR r.error_message IS NOT NULL)
                ORDER BY r.eval_case_id
                """, new MapSqlParameterSource("run_id", runId), new ColumnMapRowMapper());
        UUID newRunId = createRun(new EvalRunBatchRequest(
                oldRun.get("project_id") == null ? null : UUID.fromString(String.valueOf(oldRun.get("project_id"))),
                "重跑失败 - " + String.valueOf(oldRun.getOrDefault("run_name", runId)),
                null,
                null
        ), failedCases.size());
        updateRunStatus(newRunId, "running", null, null);
        for (Map<String, Object> row : failedCases) {
            runCase(UUID.fromString(String.valueOf(row.get("id"))), newRunId);
        }
        updateRunStats(newRunId, "success");
        return getRun(newRunId);
    }

    private Map<String, Object> runCase(UUID id, UUID runId) {
        Map<String, Object> evalCase = getCase(id);
        long start = System.nanoTime();
        try {
            RunOutput output = invokeCase(evalCase);
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            return insertResult(runId, id, evalCase, output, durationMs, null);
        } catch (Exception ex) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            // 单条用例异常也要落库，批量回归才能定位失败样本且不中断整批。
            RunOutput output = new RunOutput("", List.of(), null, null, null, null, null, null);
            return insertResult(runId, id, evalCase, output, durationMs, ex.getMessage());
        }
    }

    private RunOutput invokeCase(Map<String, Object> evalCase) {
        UUID projectId = UUID.fromString(String.valueOf(evalCase.get("project_id")));
        String question = String.valueOf(evalCase.get("question"));
        String mode = String.valueOf(evalCase.get("expected_mode"));
        if ("requirement_check".equals(mode)) {
            RequirementCheckResponse response = requirementCheckService.check(new RequirementCheckRequest(projectId, question, null));
            return new RunOutput(
                    response.conclusion(),
                    response.references(),
                    response.feasibilityLevel(),
                    response.modelProvider(),
                    response.modelName(),
                    response.promptTemplateId(),
                    response.promptTemplateName(),
                    response.searchStrategy()
            );
        }
        String safeMode = (mode == null || mode.isBlank() || "api".equals(mode) || "db_table".equals(mode)) ? "business_qa" : mode;
        ChatResponse response = chatService.chat(new ChatRequest(projectId, safeMode, question));
        return new RunOutput(
                response.answer(),
                response.references(),
                null,
                response.modelProvider(),
                response.modelName(),
                response.promptTemplateId(),
                response.promptTemplateName(),
                response.searchStrategy()
        );
    }

    private Map<String, Object> insertResult(UUID runId,
                                             UUID caseId,
                                             Map<String, Object> evalCase,
                                             RunOutput output,
                                             long durationMs,
                                             String errorMessage) {
        EvalScoreResult score = scoringService.score(evalCase, output.answer(), output.references(), output.feasibilityLevel());
        String sql = """
                INSERT INTO ai_eval_result(
                    run_id, eval_case_id, actual_answer, actual_references, actual_feasibility_level,
                    auto_score, keyword_score, source_score, refusal_score, feasibility_score, reference_count,
                    matched_keywords, matched_sources, missing_keywords, missing_sources, auto_passed, score_detail,
                    model_provider, model_name, prompt_template_id, prompt_template_name, search_strategy, duration_ms,
                    error_message, passed, score
                )
                VALUES (
                    :run_id, :eval_case_id, :actual_answer, CAST(:actual_references AS jsonb), :actual_feasibility_level,
                    :auto_score, :keyword_score, :source_score, :refusal_score, :feasibility_score, :reference_count,
                    CAST(:matched_keywords AS jsonb), CAST(:matched_sources AS jsonb), CAST(:missing_keywords AS jsonb),
                    CAST(:missing_sources AS jsonb), :auto_passed, CAST(:score_detail AS jsonb),
                    :model_provider, :model_name, :prompt_template_id, :prompt_template_name, :search_strategy, :duration_ms,
                    :error_message, :passed, :score
                )
                RETURNING *
                """;
        return normalizeRow(jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                .addValue("run_id", runId)
                .addValue("eval_case_id", caseId)
                .addValue("actual_answer", output.answer())
                .addValue("actual_references", toJson(output.references()))
                .addValue("actual_feasibility_level", output.feasibilityLevel())
                .addValue("auto_score", score.autoScore())
                .addValue("keyword_score", score.keywordScore())
                .addValue("source_score", score.sourceScore())
                .addValue("refusal_score", score.refusalScore())
                .addValue("feasibility_score", score.feasibilityScore())
                .addValue("reference_count", score.referenceCount())
                .addValue("matched_keywords", toJson(score.matchedKeywords()))
                .addValue("matched_sources", toJson(score.matchedSources()))
                .addValue("missing_keywords", toJson(score.missingKeywords()))
                .addValue("missing_sources", toJson(score.missingSources()))
                .addValue("auto_passed", errorMessage == null && score.autoPassed())
                .addValue("score_detail", toJson(score.scoreDetail()))
                .addValue("model_provider", output.modelProvider())
                .addValue("model_name", output.modelName())
                .addValue("prompt_template_id", output.promptTemplateId())
                .addValue("prompt_template_name", output.promptTemplateName())
                .addValue("search_strategy", output.searchStrategy())
                .addValue("duration_ms", durationMs)
                .addValue("error_message", errorMessage)
                .addValue("passed", errorMessage == null && score.autoPassed())
                .addValue("score", score.autoScore()), new ColumnMapRowMapper()));
    }

    private List<Map<String, Object>> queryEnabledCases(UUID projectId, String tags, String expectedMode) {
        String sql = """
                SELECT id
                FROM ai_eval_case
                WHERE enabled = TRUE
                  AND (CAST(:project_id AS uuid) IS NULL OR project_id = CAST(:project_id AS uuid))
                  AND (CAST(:expected_mode AS text) IS NULL OR expected_mode = CAST(:expected_mode AS text))
                  AND (CAST(:tags AS text) IS NULL OR CAST(tags AS text) ILIKE CAST(:tags_like AS text))
                ORDER BY created_at
                """;
        String tagText = blankToNull(tags);
        return jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("expected_mode", blankToNull(expectedMode))
                .addValue("tags", tagText)
                .addValue("tags_like", tagText == null ? null : "%" + tagText + "%"), new ColumnMapRowMapper());
    }

    private UUID createRun(EvalRunBatchRequest request, int totalCases) {
        String sql = """
                INSERT INTO ai_eval_run(run_name, project_id, status, total_cases, model_provider, model_name, search_strategy, started_at, remark)
                VALUES (:run_name, :project_id, 'pending', :total_cases, :model_provider, :model_name, :search_strategy, :started_at, :remark)
                RETURNING id
                """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                .addValue("run_name", request.runName() == null || request.runName().isBlank()
                        ? "评测运行 " + OffsetDateTime.now()
                        : request.runName())
                .addValue("project_id", request.projectId())
                .addValue("total_cases", totalCases)
                .addValue("model_provider", null)
                .addValue("model_name", null)
                .addValue("search_strategy", null)
                .addValue("started_at", null)
                .addValue("remark", request.tags() == null ? null : "tags=" + request.tags()), UUID.class);
    }

    private void updateRunStatus(UUID runId, String status, String modelProvider, String modelName) {
        jdbcTemplate.update("""
                UPDATE ai_eval_run
                SET status = :status,
                    model_provider = COALESCE(:model_provider, model_provider),
                    model_name = COALESCE(:model_name, model_name),
                    started_at = COALESCE(started_at, now()),
                    updated_at = now()
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", runId)
                .addValue("status", status)
                .addValue("model_provider", modelProvider)
                .addValue("model_name", modelName));
    }

    private void updateRunStats(UUID runId, String status) {
        jdbcTemplate.update("""
                UPDATE ai_eval_run r
                SET status = :status,
                    total_cases = s.total_cases,
                    success_cases = s.success_cases,
                    failed_cases = s.failed_cases,
                    average_score = s.average_score,
                    pass_rate = s.pass_rate,
                    model_provider = s.model_provider,
                    model_name = s.model_name,
                    prompt_template_id = s.prompt_template_id,
                    prompt_template_name = s.prompt_template_name,
                    search_strategy = s.search_strategy,
                    finished_at = now(),
                    updated_at = now()
                FROM (
                    SELECT COUNT(*) AS total_cases,
                           COUNT(*) FILTER (WHERE auto_passed = TRUE) AS success_cases,
                           COUNT(*) FILTER (WHERE auto_passed = FALSE OR error_message IS NOT NULL) AS failed_cases,
                           ROUND(COALESCE(AVG(auto_score), 0), 2) AS average_score,
                           ROUND(CASE WHEN COUNT(*) = 0 THEN 0 ELSE COUNT(*) FILTER (WHERE auto_passed = TRUE) * 100.0 / COUNT(*) END, 2) AS pass_rate,
                           MIN(model_provider) FILTER (WHERE model_provider IS NOT NULL) AS model_provider,
                           MIN(model_name) FILTER (WHERE model_name IS NOT NULL) AS model_name,
                           (array_agg(prompt_template_id) FILTER (WHERE prompt_template_id IS NOT NULL))[1] AS prompt_template_id,
                           MIN(prompt_template_name) FILTER (WHERE prompt_template_name IS NOT NULL) AS prompt_template_name,
                           MIN(search_strategy) FILTER (WHERE search_strategy IS NOT NULL) AS search_strategy
                    FROM ai_eval_result
                    WHERE run_id = :id
                ) s
                WHERE r.id = :id
                """, new MapSqlParameterSource()
                .addValue("id", runId)
                .addValue("status", status));
    }

    private Map<String, Object> getCase(UUID id) {
        return jdbcTemplate.query("SELECT * FROM ai_eval_case WHERE id = :id",
                        new MapSqlParameterSource("id", id), new ColumnMapRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评测用例不存在"));
    }

    private MapSqlParameterSource params(EvalCaseRequest request) {
        return new MapSqlParameterSource()
                .addValue("project_id", request.projectId())
                .addValue("question", request.question())
                .addValue("expected_answer", request.expectedAnswer())
                .addValue("expected_sources", jsonArray(request.expectedSources()))
                .addValue("expected_mode", request.expectedMode() == null || request.expectedMode().isBlank() ? "business_qa" : request.expectedMode())
                .addValue("expected_feasibility_level", request.expectedFeasibilityLevel())
                .addValue("expected_keywords", jsonArray(request.expectedKeywords()))
                .addValue("expected_source_titles", jsonArray(request.expectedSourceTitles()))
                .addValue("expected_source_types", jsonArray(request.expectedSourceTypes()))
                .addValue("expected_refusal", request.expectedRefusal() != null && request.expectedRefusal())
                .addValue("expected_answer_type", blankToNull(request.expectedAnswerType()))
                .addValue("score_rules", jsonObject(request.scoreRules()))
                .addValue("tags", jsonArray(request.tags()))
                .addValue("difficulty", blankToNull(request.difficulty()))
                .addValue("remark", request.remark())
                .addValue("enabled", request.enabled() == null || request.enabled());
    }

    private String jsonArray(Object value) {
        if (value == null) {
            return "[]";
        }
        if (value instanceof String text && text.trim().startsWith("[")) {
            return text;
        }
        return toJson(value instanceof String text && !text.isBlank() ? List.of(text) : value);
    }

    private String jsonObject(Object value) {
        if (value == null) {
            return "{}";
        }
        if (value instanceof String text && text.trim().startsWith("{")) {
            return text;
        }
        return toJson(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<Map<String, Object>> normalizeRows(List<Map<String, Object>> rows) {
        return rows.stream().map(this::normalizeRow).toList();
    }

    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key, normalizeValue(value)));
        return normalized;
    }

    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        String className = value.getClass().getName();
        if (className.equals("org.postgresql.util.PGobject")) {
            try {
                String text = String.valueOf(value);
                if (text.startsWith("{")) {
                    return objectMapper.readValue(text, Map.class);
                }
                if (text.startsWith("[")) {
                    return objectMapper.readValue(text, List.class);
                }
                return text;
            } catch (Exception ex) {
                return String.valueOf(value);
            }
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record RunOutput(String answer,
                             Object references,
                             String feasibilityLevel,
                             String modelProvider,
                             String modelName,
                             UUID promptTemplateId,
                             String promptTemplateName,
                             String searchStrategy) {
    }
}
