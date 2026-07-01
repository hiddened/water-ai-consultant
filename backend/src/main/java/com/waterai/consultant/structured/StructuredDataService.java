package com.waterai.consultant.structured;

import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class StructuredDataService {

    private static final List<String> AUDIT_COLUMNS = List.of("id", "created_at", "updated_at", "enabled");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<String, CrudResource> resources;

    public StructuredDataService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.resources = buildResources();
    }

    public List<Map<String, Object>> list(String resourceKey, String projectId, String moduleName, String keyword) {
        CrudResource resource = getResource(resourceKey);
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder(selectSql(resource)).append(" WHERE deleted = FALSE");

        applyProjectFilter(resource, projectId, sql, params);
        if (resource.moduleScoped() && StringUtils.hasText(moduleName)) {
            sql.append(" AND module_name = :module_name");
            params.addValue("module_name", moduleName);
        }
        if (StringUtils.hasText(keyword)) {
            appendKeywordFilter(resource, keyword, sql, params);
        }
        sql.append(" ORDER BY updated_at DESC, created_at DESC");
        return jdbcTemplate.query(sql.toString(), params, new ColumnMapRowMapper());
    }

    public Map<String, Object> get(String resourceKey, UUID id) {
        CrudResource resource = getResource(resourceKey);
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        String sql = selectSql(resource) + " WHERE id = :id AND deleted = FALSE";
        return jdbcTemplate.query(sql, params, new ColumnMapRowMapper()).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "数据不存在"));
    }

    public Map<String, Object> create(String resourceKey, Map<String, Object> request) {
        CrudResource resource = getResource(resourceKey);
        Map<String, Object> values = sanitizeWritableValues(resource, request);
        values.putIfAbsent("enabled", Boolean.TRUE);
        if (values.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求字段不能为空");
        }

        List<String> columns = new ArrayList<>(values.keySet());
        MapSqlParameterSource params = toSqlParameters(resource, values);
        String sql = "INSERT INTO " + resource.tableName()
                + " (" + String.join(", ", columns) + ") VALUES ("
                + String.join(", ", columns.stream().map(column -> valueExpression(resource, column)).toList())
                + ") RETURNING " + String.join(", ", resource.selectColumns());
        return jdbcTemplate.queryForObject(sql, params, new ColumnMapRowMapper());
    }

    public Map<String, Object> update(String resourceKey, UUID id, Map<String, Object> request) {
        CrudResource resource = getResource(resourceKey);
        Map<String, Object> values = sanitizeWritableValues(resource, request);
        MapSqlParameterSource params = toSqlParameters(resource, values);
        params.addValue("id", id);

        List<String> assignments = new ArrayList<>();
        for (String column : values.keySet()) {
            assignments.add(column + " = " + valueExpression(resource, column));
        }
        assignments.add("updated_at = now()");

        String sql = "UPDATE " + resource.tableName()
                + " SET " + String.join(", ", assignments)
                + " WHERE id = :id AND deleted = FALSE RETURNING "
                + String.join(", ", resource.selectColumns());
        return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, new ColumnMapRowMapper()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "数据不存在"));
    }

    public void delete(String resourceKey, UUID id) {
        CrudResource resource = getResource(resourceKey);
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        String sql = "UPDATE " + resource.tableName()
                + " SET deleted = TRUE, enabled = FALSE, updated_at = now()"
                + " WHERE id = :id AND deleted = FALSE";
        int affected = jdbcTemplate.update(sql, params);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据不存在");
        }
    }

    private CrudResource getResource(String resourceKey) {
        CrudResource resource = resources.get(resourceKey);
        if (resource == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未知资源类型");
        }
        return resource;
    }

    private void applyProjectFilter(CrudResource resource, String projectId, StringBuilder sql, MapSqlParameterSource params) {
        if (!StringUtils.hasText(projectId)) {
            return;
        }

        UUID projectUuid = parseUuid(projectId, "project_id 格式不合法");
        if (resource.projectScoped()) {
            sql.append(" AND project_id = :project_id");
            params.addValue("project_id", projectUuid);
            return;
        }

        // 项目表没有 project_id 字段，列表传 project_id 时按自身 id 精确过滤，保持前端筛选参数统一。
        if ("ai_project".equals(resource.tableName())) {
            sql.append(" AND id = :project_id");
            params.addValue("project_id", projectUuid);
        }
    }

    private void appendKeywordFilter(CrudResource resource, String keyword, StringBuilder sql, MapSqlParameterSource params) {
        List<String> clauses = resource.keywordColumns().stream()
                .map(column -> "LOWER(COALESCE(CAST(" + column + " AS TEXT), '')) LIKE :keyword")
                .toList();
        if (clauses.isEmpty()) {
            return;
        }
        sql.append(" AND (").append(String.join(" OR ", clauses)).append(")");
        params.addValue("keyword", "%" + keyword.toLowerCase(Locale.ROOT) + "%");
    }

    private String selectSql(CrudResource resource) {
        return "SELECT " + String.join(", ", resource.selectColumns()) + " FROM " + resource.tableName();
    }

    private Map<String, Object> sanitizeWritableValues(CrudResource resource, Map<String, Object> request) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String column : resource.writableColumns()) {
            if (request.containsKey(column)) {
                values.put(column, normalizeValue(resource, column, request.get(column)));
            }
        }
        return values;
    }

    private Object normalizeValue(CrudResource resource, String column, Object value) {
        if (value instanceof String text && !StringUtils.hasText(text)) {
            return null;
        }
        if (resource.uuidColumns().contains(column) && value instanceof String text) {
            return parseUuid(text, column + " 格式不合法");
        }
        if (resource.jsonColumns().contains(column) && value == null) {
            return "[]";
        }
        return value;
    }

    private UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private MapSqlParameterSource toSqlParameters(CrudResource resource, Map<String, Object> values) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            params.addValue(entry.getKey(), entry.getValue());
        }
        return params;
    }

    private String valueExpression(CrudResource resource, String column) {
        if (resource.uuidColumns().contains(column)) {
            return "CAST(:" + column + " AS uuid)";
        }
        if (resource.jsonColumns().contains(column)) {
            return "CAST(:" + column + " AS jsonb)";
        }
        if (resource.timestampColumns().contains(column)) {
            return "CAST(:" + column + " AS timestamptz)";
        }
        return ":" + column;
    }

    private Map<String, CrudResource> buildResources() {
        Map<String, CrudResource> map = new HashMap<>();
        map.put("projects", resource(
                "ai_project",
                List.of("project_code", "project_name", "customer_name", "industry", "description", "status", "enabled"),
                List.of("project_code", "project_name", "customer_name", "industry", "description", "status"),
                Set.of(),
                Set.of(),
                Set.of(),
                false,
                false
        ));
        map.put("documents", resource(
                "ai_document",
                List.of("project_id", "document_name", "module_name", "document_type", "file_path", "file_hash", "file_size",
                        "storage_type", "storage_bucket", "storage_object_key", "content_type",
                        "parse_status", "parse_error", "last_parsed_at", "index_status", "index_error", "last_indexed_at", "chunk_count",
                        "uploaded_by", "uploaded_at", "enabled"),
                List.of("document_name", "module_name", "document_type", "file_path", "file_hash", "parse_status",
                        "storage_type", "storage_bucket", "storage_object_key", "index_status", "index_error", "uploaded_by"),
                Set.of("project_id"),
                Set.of(),
                Set.of("uploaded_at", "last_parsed_at", "last_indexed_at"),
                true,
                true
        ));
        map.put("pages", resource(
                "ai_page",
                List.of("project_id", "page_name", "module_name", "route_path", "operation_desc", "business_rule", "keywords", "enabled"),
                List.of("page_name", "module_name", "route_path", "operation_desc", "business_rule", "keywords"),
                Set.of("project_id"),
                Set.of(),
                Set.of(),
                true,
                true
        ));
        map.put("capabilities", resource(
                "ai_capability",
                List.of("project_id", "capability_name", "module_name", "support_level", "config_required", "description", "limitation", "keywords", "enabled"),
                List.of("capability_name", "module_name", "support_level", "description", "limitation", "keywords"),
                Set.of("project_id"),
                Set.of(),
                Set.of(),
                true,
                true
        ));
        map.put("apis", resource(
                "ai_api",
                List.of("project_id", "api_name", "module_name", "method", "path", "request_desc", "response_desc", "auth_desc", "status", "keywords", "enabled"),
                List.of("api_name", "module_name", "method", "path", "request_desc", "response_desc", "auth_desc", "status", "keywords"),
                Set.of("project_id"),
                Set.of(),
                Set.of(),
                true,
                true
        ));
        map.put("db-tables", resource(
                "ai_db_table",
                List.of("project_id", "table_name", "table_comment", "module_name", "field_desc", "relation_desc", "keywords", "enabled"),
                List.of("table_name", "table_comment", "module_name", "field_desc", "relation_desc", "keywords"),
                Set.of("project_id"),
                Set.of("field_desc"),
                Set.of(),
                true,
                true
        ));
        map.put("requirement-cases", resource(
                "ai_requirement_case",
                List.of("project_id", "case_title", "module_name", "requirement_desc", "solution_desc", "feasibility_level",
                        "workload_level", "risk_points", "keywords", "enabled"),
                List.of("case_title", "module_name", "requirement_desc", "solution_desc", "feasibility_level", "workload_level", "risk_points", "keywords"),
                Set.of("project_id"),
                Set.of(),
                Set.of(),
                true,
                true
        ));
        return Map.copyOf(map);
    }

    private CrudResource resource(String tableName,
                                  List<String> writableColumns,
                                  List<String> keywordColumns,
                                  Set<String> uuidColumns,
                                  Set<String> jsonColumns,
                                  Set<String> timestampColumns,
                                  boolean projectScoped,
                                  boolean moduleScoped) {
        List<String> selectColumns = new ArrayList<>(AUDIT_COLUMNS);
        if (projectScoped) {
            selectColumns.add("project_id");
        }
        selectColumns.addAll(writableColumns.stream().filter(column -> !selectColumns.contains(column)).toList());
        return new CrudResource(tableName, writableColumns, selectColumns, keywordColumns, uuidColumns, jsonColumns,
                timestampColumns, projectScoped, moduleScoped);
    }
}
