package com.waterai.consultant.chat;

import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatSessionService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChatSessionService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> list(UUID projectId, String mode, String keyword, String startTime, String endTime) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT s.id AS session_id,
                       s.project_id,
                       p.project_name,
                       s.mode,
                       COUNT(*) FILTER (WHERE m.role = 'user')::int AS question_count,
                       (
                           SELECT um.content FROM ai_chat_message um
                           WHERE um.session_id = s.id AND um.role = 'user'
                           ORDER BY um.created_at DESC LIMIT 1
                       ) AS last_question,
                       (
                           SELECT LEFT(am.content, 220) FROM ai_chat_message am
                           WHERE am.session_id = s.id AND am.role = 'assistant'
                           ORDER BY am.created_at DESC LIMIT 1
                       ) AS last_answer_preview,
                       s.created_at,
                       s.updated_at
                FROM ai_chat_session s
                JOIN ai_project p ON p.id = s.project_id
                LEFT JOIN ai_chat_message m ON m.session_id = s.id
                WHERE s.deleted = FALSE
                """);
        if (projectId != null) {
            sql.append("\n AND s.project_id = :project_id\n");
            params.addValue("project_id", projectId);
        }
        if (mode != null && !mode.isBlank()) {
            sql.append("\n AND s.mode = :mode\n");
            params.addValue("mode", mode);
        }
        if (startTime != null && !startTime.isBlank()) {
            sql.append("\n AND s.created_at >= CAST(:start_time AS timestamptz)\n");
            params.addValue("start_time", startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            sql.append("\n AND s.created_at <= CAST(:end_time AS timestamptz)\n");
            params.addValue("end_time", endTime);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1 FROM ai_chat_message km
                         WHERE km.session_id = s.id AND km.content ILIKE :keyword
                     )
                    """);
            params.addValue("keyword", "%" + keyword + "%");
        }
        sql.append("""
                GROUP BY s.id, p.project_name
                ORDER BY s.updated_at DESC
                LIMIT 200
                """);
        return jdbcTemplate.query(sql.toString(), params, new ColumnMapRowMapper());
    }

    public Map<String, Object> get(UUID sessionId) {
        return jdbcTemplate.query("""
                        SELECT s.id AS session_id, s.project_id, p.project_name, s.title, s.mode, s.created_at, s.updated_at
                        FROM ai_chat_session s
                        JOIN ai_project p ON p.id = s.project_id
                        WHERE s.id = :id AND s.deleted = FALSE
                        """, new MapSqlParameterSource("id", sessionId), new ColumnMapRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "对话不存在"));
    }

    public List<Map<String, Object>> messages(UUID sessionId) {
        get(sessionId);
        List<Map<String, Object>> messages = jdbcTemplate.query("""
                SELECT id, session_id, project_id, role, content, confidence, trace_id,
                       llm_provider, model_config_id, model_provider, model_name,
                       prompt_template_id, prompt_template_name, prompt_rendered_preview,
                       search_strategy, insufficient_answer, created_at
                FROM ai_chat_message
                WHERE session_id = :session_id
                ORDER BY created_at ASC
                """, new MapSqlParameterSource("session_id", sessionId), new ColumnMapRowMapper());
        for (Map<String, Object> message : messages) {
            Object role = message.get("role");
            if ("assistant".equals(String.valueOf(role))) {
                message.put("references", references(UUID.fromString(String.valueOf(message.get("id")))));
            } else {
                message.put("references", List.of());
            }
        }
        return messages;
    }

    public void delete(UUID sessionId) {
        int updated = jdbcTemplate.update("""
                UPDATE ai_chat_session
                SET deleted = TRUE, updated_at = now()
                WHERE id = :id AND deleted = FALSE
                """, new MapSqlParameterSource("id", sessionId));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对话不存在");
        }
    }

    private List<Map<String, Object>> references(UUID messageId) {
        return jdbcTemplate.query("""
                SELECT id, message_id, project_id, source_type, source_id,
                       source_title AS title,
                       source_title,
                       source_locator,
                       quote AS content_preview,
                       quote AS content,
                       quote,
                       score,
                       created_at
                FROM ai_answer_reference
                WHERE message_id = :message_id
                ORDER BY score DESC NULLS LAST, created_at ASC
                """, new MapSqlParameterSource("message_id", messageId), new ColumnMapRowMapper());
    }
}
