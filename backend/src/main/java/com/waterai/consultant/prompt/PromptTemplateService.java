package com.waterai.consultant.prompt;

import com.waterai.consultant.chat.ChatService;
import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PromptTemplateService {

    private static final Set<String> TEMPLATE_TYPES = Set.of("chat", "requirement_check", "page_help", "business_qa", "doc_qa");
    private static final Set<String> OUTPUT_FORMATS = Set.of("markdown", "json", "structured_json");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PromptTemplateService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> list(String templateType, String mode, Boolean enabled) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT id, template_name, template_type, mode, system_prompt, user_prompt_template,
                       output_format, enabled, default_template, remark, created_at, updated_at
                FROM ai_prompt_template
                WHERE deleted = FALSE
                """);
        if (templateType != null && !templateType.isBlank()) {
            sql.append("\n AND template_type = :template_type\n");
            params.addValue("template_type", templateType);
        }
        if (mode != null && !mode.isBlank()) {
            sql.append("\n AND mode = :mode\n");
            params.addValue("mode", mode);
        }
        if (enabled != null) {
            sql.append("\n AND enabled = :enabled\n");
            params.addValue("enabled", enabled);
        }
        sql.append(" ORDER BY template_type, mode NULLS LAST, default_template DESC, updated_at DESC");
        return jdbcTemplate.query(sql.toString(), params, new ColumnMapRowMapper());
    }

    public Map<String, Object> get(UUID id) {
        return jdbcTemplate.query("""
                        SELECT id, template_name, template_type, mode, system_prompt, user_prompt_template,
                               output_format, enabled, default_template, remark, created_at, updated_at
                        FROM ai_prompt_template
                        WHERE id = :id AND deleted = FALSE
                        """, new MapSqlParameterSource("id", id), new ColumnMapRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Prompt 模板不存在"));
    }

    public Map<String, Object> create(PromptTemplateRequest request) {
        validate(request);
        UUID id = UUID.randomUUID();
        boolean defaultTemplate = Boolean.TRUE.equals(request.defaultTemplate());
        if (defaultTemplate) {
            clearDefault(request.templateType(), request.mode());
        }
        jdbcTemplate.update("""
                INSERT INTO ai_prompt_template(id, template_name, template_type, mode, system_prompt, user_prompt_template,
                                               output_format, enabled, default_template, remark)
                VALUES (:id, :template_name, :template_type, :mode, :system_prompt, :user_prompt_template,
                        :output_format, :enabled, :default_template, :remark)
                """, params(id, request)
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("default_template", defaultTemplate));
        return get(id);
    }

    public Map<String, Object> update(UUID id, PromptTemplateRequest request) {
        get(id);
        validate(request);
        boolean defaultTemplate = Boolean.TRUE.equals(request.defaultTemplate());
        if (defaultTemplate) {
            clearDefault(request.templateType(), request.mode());
        }
        jdbcTemplate.update("""
                UPDATE ai_prompt_template
                SET template_name = :template_name,
                    template_type = :template_type,
                    mode = :mode,
                    system_prompt = :system_prompt,
                    user_prompt_template = :user_prompt_template,
                    output_format = :output_format,
                    enabled = :enabled,
                    default_template = :default_template,
                    remark = :remark,
                    updated_at = now()
                WHERE id = :id AND deleted = FALSE
                """, params(id, request)
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("default_template", defaultTemplate));
        return get(id);
    }

    public void delete(UUID id) {
        int updated = jdbcTemplate.update("""
                UPDATE ai_prompt_template
                SET enabled = FALSE, default_template = FALSE, deleted = TRUE, updated_at = now()
                WHERE id = :id AND deleted = FALSE
                """, new MapSqlParameterSource("id", id));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Prompt 模板不存在");
        }
    }

    public Map<String, Object> setDefault(UUID id) {
        Map<String, Object> template = get(id);
        clearDefault(String.valueOf(template.get("template_type")), string(template.get("mode")));
        jdbcTemplate.update("""
                UPDATE ai_prompt_template
                SET default_template = TRUE, enabled = TRUE, updated_at = now()
                WHERE id = :id AND deleted = FALSE
                """, new MapSqlParameterSource("id", id));
        return get(id);
    }

    public Map<String, Object> test(UUID id, PromptTemplateTestRequest request) {
        Map<String, Object> template = get(id);
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("question", fallback(request.question(), "如何新增项目？"));
        variables.put("project_name", fallback(request.projectName(), "示例项目"));
        variables.put("mode", fallback(request.mode(), string(template.get("mode"))));
        variables.put("context", fallback(request.context(), "暂无"));
        variables.put("references", fallback(request.references(), "暂无"));
        variables.put("related_pages", "暂无");
        variables.put("related_capabilities", "暂无");
        variables.put("related_apis", "暂无");
        variables.put("related_tables", "暂无");
        variables.put("requirement_desc", fallback(request.requirementDesc(), "客户希望新增一个水务业务能力。"));
        variables.put("module_name", fallback(request.moduleName(), "暂无"));
        variables.put("current_time", OffsetDateTime.now().toString());
        return Map.of(
                "template_id", id,
                "template_name", template.get("template_name"),
                "rendered_system_prompt", render(string(template.get("system_prompt")), variables),
                "rendered_user_prompt", render(string(template.get("user_prompt_template")), variables)
        );
    }

    public PromptRenderResult renderChat(UUID projectId, String mode, String question, List<KnowledgeEvidence> evidences) {
        Map<String, Object> template = findTemplate(mode, mode).orElseGet(() -> builtIn(mode));
        Map<String, String> variables = baseVariables(projectId, mode, question, null, null, evidences);
        return new PromptRenderResult(
                toUuid(template.get("id")),
                string(template.get("template_name")),
                render(string(template.get("system_prompt")), variables),
                render(string(template.get("user_prompt_template")), variables),
                string(template.get("output_format"))
        );
    }

    public PromptRenderResult renderRequirement(UUID projectId, String requirementDesc, String moduleName, List<KnowledgeEvidence> evidences) {
        Map<String, Object> template = findTemplate("requirement_check", "requirement_check").orElseGet(() -> builtIn("requirement_check"));
        Map<String, String> variables = baseVariables(projectId, "requirement_check", requirementDesc, requirementDesc, moduleName, evidences);
        return new PromptRenderResult(
                toUuid(template.get("id")),
                string(template.get("template_name")),
                render(string(template.get("system_prompt")), variables),
                render(string(template.get("user_prompt_template")), variables),
                string(template.get("output_format"))
        );
    }

    private java.util.Optional<Map<String, Object>> findTemplate(String templateType, String mode) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT *
                FROM ai_prompt_template
                WHERE deleted = FALSE
                  AND enabled = TRUE
                  AND default_template = TRUE
                  AND (template_type = :template_type OR template_type = 'chat')
                  AND (mode = :mode OR mode IS NULL OR mode = '')
                ORDER BY CASE WHEN template_type = :template_type THEN 0 ELSE 1 END,
                         CASE WHEN mode = :mode THEN 0 ELSE 1 END,
                         updated_at DESC
                LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("template_type", templateType)
                .addValue("mode", mode), new ColumnMapRowMapper());
        return rows.stream().findFirst();
    }

    private Map<String, String> baseVariables(UUID projectId,
                                              String mode,
                                              String question,
                                              String requirementDesc,
                                              String moduleName,
                                              List<KnowledgeEvidence> evidences) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("question", fallback(question, ""));
        variables.put("project_name", projectName(projectId));
        variables.put("mode", fallback(mode, ""));
        variables.put("context", formatEvidence(evidences));
        variables.put("references", formatReferences(evidences));
        variables.put("related_pages", formatBySource(evidences, "PAGE"));
        variables.put("related_capabilities", formatBySource(evidences, "CAPABILITY"));
        variables.put("related_apis", formatBySource(evidences, "API"));
        variables.put("related_tables", formatBySource(evidences, "DB_TABLE"));
        variables.put("requirement_desc", fallback(requirementDesc, question));
        variables.put("module_name", fallback(moduleName, ""));
        variables.put("current_time", OffsetDateTime.now().toString());
        return variables;
    }

    private Map<String, Object> builtIn(String mode) {
        return switch (mode) {
            case "page_help" -> template("内置页面操作模板", "page_help", mode, "markdown",
                    "你是 AI 水务项目页面操作顾问。只能基于 references 和检索上下文回答。",
                    """
                            项目：{{project_name}}
                            问题：{{question}}
                            页面说明优先依据：{{related_pages}}
                            检索上下文：{{context}}
                            要求：输出操作步骤；缺少页面说明时，不得编造页面路径、按钮和菜单。
                            """);
            case "business_qa" -> template("内置业务解答模板", "business_qa", mode, "markdown",
                    "你是 AI 水务项目业务顾问。只能基于 references 和检索上下文回答。",
                    """
                            项目：{{project_name}}
                            问题：{{question}}
                            能力/接口/历史案例：{{related_capabilities}}\n{{related_apis}}\n{{references}}
                            检索上下文：{{context}}
                            如果涉及当前系统是否支持，必须有依据；没有依据必须回答“%s”。
                            """.formatted(ChatService.INSUFFICIENT_ANSWER));
            case "requirement_check" -> template("内置需求分析模板", "requirement_check", "requirement_check", "structured_json",
                    "你是 AI 水务项目智能顾问。只基于 references 和检索上下文输出严格 JSON。",
                    """
                            需求：{{requirement_desc}}
                            模块：{{module_name}}
                            能力清单：{{related_capabilities}}
                            历史/引用：{{references}}
                            检索上下文：{{context}}
                            输出 JSON 字段必须包含 requirement_understanding, feasibility_level, conclusion, matched_capabilities, missing_capabilities, related_pages, related_apis, related_tables, impact_modules, risk_points, recommended_solution, workload_level, references。
                            规则：没有能力清单或历史案例依据不能直接判断 A/B；没有任何依据必须判断 D，并说明资料不足。
                            """);
            default -> template("内置文档问答模板", "doc_qa", mode, "markdown",
                    "你是 AI 水务项目智能顾问。只能基于 references 和检索上下文回答。",
                    """
                            项目：{{project_name}}
                            模式：{{mode}}
                            问题：{{question}}
                            references：{{references}}
                            检索上下文：{{context}}
                            如果没有依据，必须回答“%s”。回答要列出引用来源。
                            """.formatted(ChatService.INSUFFICIENT_ANSWER));
        };
    }

    private Map<String, Object> template(String name, String type, String mode, String outputFormat, String system, String user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", null);
        map.put("template_name", name);
        map.put("template_type", type);
        map.put("mode", mode);
        map.put("output_format", outputFormat);
        map.put("system_prompt", system);
        map.put("user_prompt_template", user);
        return map;
    }

    private String render(String template, Map<String, String> variables) {
        String result = template == null ? "" : template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", fallback(entry.getValue(), "暂无"));
        }
        return result;
    }

    private String formatEvidence(List<KnowledgeEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return "暂无";
        }
        AtomicInteger index = new AtomicInteger(1);
        StringBuilder builder = new StringBuilder();
        for (KnowledgeEvidence evidence : evidences) {
            builder.append("[R").append(index.getAndIncrement()).append("] ")
                    .append(evidence.sourceType()).append(" / ")
                    .append(evidence.sourceTitle()).append(" / ")
                    .append(evidence.sourceLocator()).append("\n")
                    .append(evidence.content()).append("\n\n");
        }
        return builder.toString();
    }

    private String formatReferences(List<KnowledgeEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return "暂无";
        }
        return evidences.stream()
                .map(evidence -> evidence.sourceType() + " / " + evidence.sourceTitle() + " / " + evidence.sourceLocator())
                .toList()
                .toString();
    }

    private String formatBySource(List<KnowledgeEvidence> evidences, String sourceType) {
        if (evidences == null) {
            return "暂无";
        }
        String text = evidences.stream()
                .filter(evidence -> sourceType.equals(evidence.sourceType()))
                .map(evidence -> evidence.sourceTitle() + "：" + evidence.content())
                .limit(6)
                .reduce("", (left, right) -> left + "\n" + right)
                .trim();
        return text.isBlank() ? "暂无" : text;
    }

    private String projectName(UUID projectId) {
        if (projectId == null) {
            return "";
        }
        return jdbcTemplate.query("""
                        SELECT project_name FROM ai_project WHERE id = :id
                        """, new MapSqlParameterSource("id", projectId), (rs, rowNum) -> rs.getString("project_name"))
                .stream()
                .findFirst()
                .orElse("");
    }

    private MapSqlParameterSource params(UUID id, PromptTemplateRequest request) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("template_name", request.templateName())
                .addValue("template_type", request.templateType())
                .addValue("mode", blankToNull(request.mode()))
                .addValue("system_prompt", blankToNull(request.systemPrompt()))
                .addValue("user_prompt_template", request.userPromptTemplate())
                .addValue("output_format", blankToNull(request.outputFormat()))
                .addValue("remark", blankToNull(request.remark()));
    }

    private void validate(PromptTemplateRequest request) {
        if (request.templateName() == null || request.templateName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "template_name 不能为空");
        }
        if (!TEMPLATE_TYPES.contains(request.templateType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "template_type 不支持");
        }
        if (request.userPromptTemplate() == null || request.userPromptTemplate().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "user_prompt_template 不能为空");
        }
        if (request.outputFormat() != null && !request.outputFormat().isBlank() && !OUTPUT_FORMATS.contains(request.outputFormat())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "output_format 不支持");
        }
    }

    private void clearDefault(String templateType, String mode) {
        jdbcTemplate.update("""
                UPDATE ai_prompt_template
                SET default_template = FALSE, updated_at = now()
                WHERE template_type = :template_type
                  AND COALESCE(mode, '') = COALESCE(:mode, '')
                  AND deleted = FALSE
                """, new MapSqlParameterSource()
                .addValue("template_type", templateType)
                .addValue("mode", blankToNull(mode)));
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private UUID toUuid(Object value) {
        return value == null ? null : UUID.fromString(String.valueOf(value));
    }
}
