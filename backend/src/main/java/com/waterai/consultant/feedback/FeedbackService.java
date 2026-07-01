package com.waterai.consultant.feedback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedbackService {

    private static final Set<String> FEEDBACK_TYPES = Set.of("useful", "not_useful", "wrong", "incomplete", "no_source", "other");
    private static final Set<String> REVIEW_STATUS = Set.of("pending", "accepted", "rejected", "converted");
    private static final Set<String> KNOWLEDGE_TYPES = Set.of("document_faq", "page", "capability", "api", "db_table", "requirement_case");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public FeedbackService(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> list(UUID projectId, String feedbackType, String reviewStatus, String keyword, String startTime, String endTime) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT f.id, f.project_id, p.project_name, f.session_id, f.message_id,
                       f.feedback_type, f.remark, f.corrected_answer, f.expected_sources,
                       f.convert_to_knowledge, f.target_knowledge_type, f.review_status,
                       f.reviewer, f.review_remark, f.created_at, f.updated_at,
                       (
                           SELECT q.content FROM ai_chat_message q
                           WHERE q.session_id = f.session_id AND q.role = 'user'
                             AND q.created_at <= COALESCE(a.created_at, now())
                           ORDER BY q.created_at DESC LIMIT 1
                       ) AS question,
                       a.content AS answer,
                       a.trace_id,
                       a.llm_provider,
                       a.model_provider,
                       a.model_name,
                       a.prompt_template_id,
                       a.prompt_template_name,
                       a.search_strategy
                FROM ai_feedback f
                LEFT JOIN ai_project p ON p.id = f.project_id
                LEFT JOIN ai_chat_message a ON a.id = f.message_id
                WHERE 1 = 1
                """);
        if (projectId != null) {
            sql.append("\n AND f.project_id = :project_id\n");
            params.addValue("project_id", projectId);
        }
        if (feedbackType != null && !feedbackType.isBlank()) {
            sql.append("\n AND f.feedback_type = :feedback_type\n");
            params.addValue("feedback_type", feedbackType);
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            sql.append("\n AND f.review_status = :review_status\n");
            params.addValue("review_status", reviewStatus);
        }
        if (startTime != null && !startTime.isBlank()) {
            sql.append("\n AND f.created_at >= CAST(:start_time AS timestamptz)\n");
            params.addValue("start_time", startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            sql.append("\n AND f.created_at <= CAST(:end_time AS timestamptz)\n");
            params.addValue("end_time", endTime);
        }
        if (keyword != null && !keyword.isBlank()) {
            // 关键词同时覆盖反馈内容、AI 回答和对应的最近用户问题，避免列表筛选漏掉真实问题。
            sql.append("""
                     AND (f.remark ILIKE :keyword
                          OR f.corrected_answer ILIKE :keyword
                          OR a.content ILIKE :keyword
                          OR EXISTS (
                              SELECT 1 FROM ai_chat_message q
                              WHERE q.session_id = f.session_id AND q.role = 'user'
                                AND q.created_at <= COALESCE(a.created_at, now())
                                AND q.content ILIKE :keyword
                          ))
                    """);
            params.addValue("keyword", "%" + keyword + "%");
        }
        sql.append("""
                ORDER BY f.created_at DESC
                LIMIT 200
                """);
        List<Map<String, Object>> rows = jdbcTemplate.query(sql.toString(), params, new ColumnMapRowMapper());
        rows.forEach(this::normalizeJsonFields);
        return rows;
    }

    public Map<String, Object> get(UUID id) {
        Map<String, Object> feedback = jdbcTemplate.query("""
                        SELECT f.*, p.project_name, a.content AS answer, a.trace_id, a.llm_provider,
                               a.model_provider, a.model_name, a.prompt_template_id, a.prompt_template_name, a.search_strategy,
                               (
                                   SELECT q.content FROM ai_chat_message q
                                   WHERE q.session_id = f.session_id AND q.role = 'user'
                                   ORDER BY q.created_at DESC LIMIT 1
                               ) AS question
                        FROM ai_feedback f
                        LEFT JOIN ai_project p ON p.id = f.project_id
                        LEFT JOIN ai_chat_message a ON a.id = f.message_id
                        WHERE f.id = :id
                        """, new MapSqlParameterSource("id", id), new ColumnMapRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "反馈不存在"));
        normalizeJsonFields(feedback);
        feedback.put("references", references(toUuid(feedback.get("message_id"))));
        return feedback;
    }

    public Map<String, Object> create(FeedbackRequest request) {
        if (request.projectId() == null || request.messageId() == null || request.sessionId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "project_id、session_id、message_id 不能为空");
        }
        if (!FEEDBACK_TYPES.contains(request.feedbackType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "feedback_type 不支持");
        }
        String targetType = request.targetKnowledgeType();
        if (Boolean.TRUE.equals(request.convertToKnowledge()) && (targetType == null || !KNOWLEDGE_TYPES.contains(targetType))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "target_knowledge_type 不支持");
        }
        UUID id = jdbcTemplate.queryForObject("""
                INSERT INTO ai_feedback(project_id, session_id, message_id, feedback_type, remark, corrected_answer,
                                        expected_sources, convert_to_knowledge, target_knowledge_type, review_status)
                VALUES (:project_id, :session_id, :message_id, :feedback_type, :remark, :corrected_answer,
                        CAST(:expected_sources AS jsonb), :convert_to_knowledge, :target_knowledge_type, 'pending')
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("project_id", request.projectId())
                .addValue("session_id", request.sessionId())
                .addValue("message_id", request.messageId())
                .addValue("feedback_type", request.feedbackType())
                .addValue("remark", request.remark())
                .addValue("corrected_answer", request.correctedAnswer())
                .addValue("expected_sources", toJson(request.expectedSources() == null ? List.of() : request.expectedSources()))
                .addValue("convert_to_knowledge", Boolean.TRUE.equals(request.convertToKnowledge()))
                .addValue("target_knowledge_type", targetType), UUID.class);
        return get(id);
    }

    public Map<String, Object> review(UUID id, FeedbackReviewRequest request) {
        String status = request.reviewStatus();
        if (!REVIEW_STATUS.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "review_status 不支持");
        }
        jdbcTemplate.update("""
                UPDATE ai_feedback
                SET review_status = :review_status,
                    reviewer = :reviewer,
                    review_remark = :review_remark,
                    target_knowledge_type = COALESCE(:target_knowledge_type, target_knowledge_type),
                    updated_at = now()
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("review_status", status)
                .addValue("reviewer", request.reviewer())
                .addValue("review_remark", request.reviewRemark())
                .addValue("target_knowledge_type", request.targetKnowledgeType()));
        return get(id);
    }

    public Map<String, Object> convert(UUID id) {
        Map<String, Object> feedback = get(id);
        String targetType = string(feedback.get("target_knowledge_type"));
        if (targetType == null || !KNOWLEDGE_TYPES.contains(targetType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择可转入的知识类型");
        }
        UUID projectId = toUuid(feedback.get("project_id"));
        String question = fallback(string(feedback.get("question")), "反馈沉淀问题");
        String answer = fallback(string(feedback.get("corrected_answer")), fallback(string(feedback.get("answer")), string(feedback.get("remark"))));

        switch (targetType) {
            case "document_faq" -> insertFaq(projectId, question, answer, id);
            case "page" -> insertPage(projectId, question, answer, id);
            case "capability" -> insertCapability(projectId, question, answer, id);
            case "api" -> insertApi(projectId, question, answer, id);
            case "db_table" -> insertDbTable(projectId, question, answer, id);
            case "requirement_case" -> insertRequirementCase(projectId, question, answer, id);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "target_knowledge_type 不支持");
        }
        jdbcTemplate.update("""
                UPDATE ai_feedback
                SET review_status = 'converted', convert_to_knowledge = TRUE, updated_at = now()
                WHERE id = :id
                """, new MapSqlParameterSource("id", id));
        return get(id);
    }

    private List<Map<String, Object>> references(UUID messageId) {
        if (messageId == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT source_type, source_id, source_title AS title, source_title, source_locator,
                       quote AS content_preview, quote AS content, quote, score
                FROM ai_answer_reference
                WHERE message_id = :message_id
                ORDER BY score DESC NULLS LAST
                """, new MapSqlParameterSource("message_id", messageId), new ColumnMapRowMapper());
    }

    private void insertFaq(UUID projectId, String question, String answer, UUID feedbackId) {
        jdbcTemplate.update("""
                INSERT INTO ai_faq(project_id, question, answer, source_feedback_id, enabled)
                VALUES (:project_id, :question, :answer, :source_feedback_id, FALSE)
                """, params(projectId, feedbackId).addValue("question", question).addValue("answer", answer));
    }

    private void insertPage(UUID projectId, String question, String answer, UUID feedbackId) {
        jdbcTemplate.update("""
                INSERT INTO ai_page(project_id, page_name, module_name, operation_desc, business_rule, keywords, source_feedback_id, enabled)
                VALUES (:project_id, :title, '反馈沉淀', :answer, :question, :keywords, :source_feedback_id, FALSE)
                """, params(projectId, feedbackId).addValue("title", title(question)).addValue("answer", answer).addValue("question", question).addValue("keywords", question));
    }

    private void insertCapability(UUID projectId, String question, String answer, UUID feedbackId) {
        jdbcTemplate.update("""
                INSERT INTO ai_capability(project_id, capability_name, module_name, support_level, config_required, description, limitation, keywords, source_feedback_id, enabled)
                VALUES (:project_id, :title, '反馈沉淀', 'custom_required', FALSE, :answer, :question, :keywords, :source_feedback_id, FALSE)
                """, params(projectId, feedbackId).addValue("title", title(question)).addValue("answer", answer).addValue("question", question).addValue("keywords", question));
    }

    private void insertApi(UUID projectId, String question, String answer, UUID feedbackId) {
        jdbcTemplate.update("""
                INSERT INTO ai_api(project_id, api_name, module_name, method, path, request_desc, response_desc, status, keywords, source_feedback_id, enabled)
                VALUES (:project_id, :title, '反馈沉淀', 'POST', '/draft/from-feedback', :question, :answer, 'active', :keywords, :source_feedback_id, FALSE)
                """, params(projectId, feedbackId).addValue("title", title(question)).addValue("answer", answer).addValue("question", question).addValue("keywords", question));
    }

    private void insertDbTable(UUID projectId, String question, String answer, UUID feedbackId) {
        jdbcTemplate.update("""
                INSERT INTO ai_db_table(project_id, table_name, table_comment, module_name, field_desc, relation_desc, keywords, source_feedback_id, enabled)
                VALUES (:project_id, :table_name, :answer, '反馈沉淀', '[]'::jsonb, :question, :keywords, :source_feedback_id, FALSE)
                """, params(projectId, feedbackId).addValue("table_name", "draft_from_feedback").addValue("answer", answer).addValue("question", question).addValue("keywords", question));
    }

    private void insertRequirementCase(UUID projectId, String question, String answer, UUID feedbackId) {
        jdbcTemplate.update("""
                INSERT INTO ai_requirement_case(project_id, case_title, module_name, requirement_desc, solution_desc, feasibility_level, workload_level, risk_points, keywords, source_feedback_id, enabled)
                VALUES (:project_id, :title, '反馈沉淀', :question, :answer, 'D', '待评估', :remark, :keywords, :source_feedback_id, FALSE)
                """, params(projectId, feedbackId).addValue("title", title(question)).addValue("answer", answer).addValue("question", question).addValue("remark", "由反馈转入，需人工完善").addValue("keywords", question));
    }

    private MapSqlParameterSource params(UUID projectId, UUID feedbackId) {
        return new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("source_feedback_id", feedbackId);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private void normalizeJsonFields(Map<String, Object> row) {
        Object expectedSources = row.get("expected_sources");
        if (expectedSources != null) {
            row.put("expected_sources", fromJson(String.valueOf(expectedSources)));
        }
    }

    private Object fromJson(String value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(value));
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String title(String value) {
        String text = fallback(value, "反馈沉淀");
        return text.length() > 80 ? text.substring(0, 80) : text;
    }
}
