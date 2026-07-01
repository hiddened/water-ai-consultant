package com.waterai.consultant.task;

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
public class TaskService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TaskService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID create(String taskType, UUID bizId, String bizType, String message) {
        UUID taskId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO ai_task(id, task_type, biz_id, biz_type, status, progress, message)
                VALUES (:id, :task_type, :biz_id, :biz_type, 'pending', 0, :message)
                """, new MapSqlParameterSource()
                .addValue("id", taskId)
                .addValue("task_type", taskType)
                .addValue("biz_id", bizId)
                .addValue("biz_type", bizType)
                .addValue("message", message));
        log(taskId, "info", message);
        return taskId;
    }

    public List<Map<String, Object>> list(String taskType, String status, UUID bizId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT id, task_type, biz_id, biz_type, status, progress, message, error_message,
                       started_at, finished_at, created_at, updated_at
                FROM ai_task
                WHERE 1 = 1
                """);
        if (taskType != null && !taskType.isBlank()) {
            sql.append(" AND task_type = :task_type");
            params.addValue("task_type", taskType);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", status);
        }
        if (bizId != null) {
            sql.append(" AND biz_id = :biz_id");
            params.addValue("biz_id", bizId);
        }
        sql.append(" ORDER BY created_at DESC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), params, new ColumnMapRowMapper());
    }

    public Map<String, Object> get(UUID taskId) {
        return jdbcTemplate.query("""
                        SELECT id, task_type, biz_id, biz_type, status, progress, message, error_message,
                               started_at, finished_at, created_at, updated_at
                        FROM ai_task
                        WHERE id = :id
                        """, new MapSqlParameterSource("id", taskId), new ColumnMapRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));
    }

    public List<Map<String, Object>> logs(UUID taskId) {
        get(taskId);
        return jdbcTemplate.query("""
                SELECT id, task_id, log_level, message, created_at
                FROM ai_task_log
                WHERE task_id = :task_id
                ORDER BY created_at ASC
                """, new MapSqlParameterSource("task_id", taskId), new ColumnMapRowMapper());
    }

    public void start(UUID taskId, String message) {
        jdbcTemplate.update("""
                UPDATE ai_task
                SET status = 'running', progress = GREATEST(progress, 1), message = :message,
                    started_at = COALESCE(started_at, now()), updated_at = now()
                WHERE id = :id AND status IN ('pending', 'running')
                """, new MapSqlParameterSource()
                .addValue("id", taskId)
                .addValue("message", message));
        log(taskId, "info", message);
    }

    public void progress(UUID taskId, int progress, String message) {
        int bounded = Math.max(0, Math.min(99, progress));
        jdbcTemplate.update("""
                UPDATE ai_task
                SET progress = :progress, message = :message, updated_at = now()
                WHERE id = :id AND status = 'running'
                """, new MapSqlParameterSource()
                .addValue("id", taskId)
                .addValue("progress", bounded)
                .addValue("message", message));
        log(taskId, "info", message);
    }

    public void success(UUID taskId, String message) {
        jdbcTemplate.update("""
                UPDATE ai_task
                SET status = 'success', progress = 100, message = :message, error_message = NULL,
                    finished_at = now(), updated_at = now()
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", taskId)
                .addValue("message", message));
        log(taskId, "info", message);
    }

    public void fail(UUID taskId, String message, Throwable throwable) {
        String error = throwable == null ? message : throwable.getMessage();
        jdbcTemplate.update("""
                UPDATE ai_task
                SET status = 'failed', message = :message, error_message = :error_message,
                    finished_at = now(), updated_at = now()
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", taskId)
                .addValue("message", message)
                .addValue("error_message", error));
        log(taskId, "error", error == null || error.isBlank() ? message : error);
    }

    public boolean cancelPending(UUID taskId) {
        int updated = jdbcTemplate.update("""
                UPDATE ai_task
                SET status = 'cancelled', message = '任务已取消', finished_at = now(), updated_at = now()
                WHERE id = :id AND status = 'pending'
                """, new MapSqlParameterSource("id", taskId));
        if (updated > 0) {
            log(taskId, "warn", "任务已取消");
        }
        return updated > 0;
    }

    public void log(UUID taskId, String level, String message) {
        jdbcTemplate.update("""
                INSERT INTO ai_task_log(task_id, log_level, message)
                VALUES (:task_id, :log_level, :message)
                """, new MapSqlParameterSource()
                .addValue("task_id", taskId)
                .addValue("log_level", level == null || level.isBlank() ? "info" : level)
                .addValue("message", message == null ? "" : message));
    }
}
