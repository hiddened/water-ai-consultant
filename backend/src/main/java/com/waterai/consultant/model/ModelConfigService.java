package com.waterai.consultant.model;

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
public class ModelConfigService {

    private static final Set<String> PROVIDERS = Set.of("deepseek", "openai", "openai_compatible");
    private static final Set<String> MODEL_TYPES = Set.of("chat", "embedding");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ModelRuntimeService runtimeService;

    public ModelConfigService(NamedParameterJdbcTemplate jdbcTemplate, ModelRuntimeService runtimeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.runtimeService = runtimeService;
    }

    public List<Map<String, Object>> list(String provider, String modelType, Boolean enabled) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT id, config_name, provider, model_type, base_url, api_key_masked,
                       model_name, dimension, temperature, max_tokens, enabled,
                       default_config, remark, created_at, updated_at
                FROM ai_model_config
                WHERE deleted = FALSE AND provider <> 'mock'
                """);
        if (provider != null && !provider.isBlank()) {
            sql.append("\n AND provider = :provider\n");
            params.addValue("provider", provider);
        }
        if (modelType != null && !modelType.isBlank()) {
            sql.append("\n AND model_type = :model_type\n");
            params.addValue("model_type", modelType);
        }
        if (enabled != null) {
            sql.append("\n AND enabled = :enabled\n");
            params.addValue("enabled", enabled);
        }
        sql.append(" ORDER BY model_type, default_config DESC, updated_at DESC");
        return jdbcTemplate.query(sql.toString(), params, new ColumnMapRowMapper());
    }

    public Map<String, Object> get(UUID id) {
        return jdbcTemplate.query("""
                        SELECT id, config_name, provider, model_type, base_url, api_key_masked,
                               model_name, dimension, temperature, max_tokens, enabled,
                               default_config, remark, created_at, updated_at
                        FROM ai_model_config
                        WHERE id = :id AND deleted = FALSE AND provider <> 'mock'
                        """, new MapSqlParameterSource("id", id), new ColumnMapRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在"));
    }

    public Map<String, Object> create(ModelConfigRequest request) {
        validate(request);
        UUID id = UUID.randomUUID();
        boolean defaultConfig = Boolean.TRUE.equals(request.defaultConfig());
        if (defaultConfig) {
            clearDefault(request.modelType());
        }
        jdbcTemplate.update("""
                INSERT INTO ai_model_config(id, config_name, provider, model_type, base_url, api_key_value, api_key_masked,
                                            model_name, dimension, temperature, max_tokens, enabled, default_config, remark)
                VALUES (:id, :config_name, :provider, :model_type, :base_url, :api_key_value, :api_key_masked,
                        :model_name, :dimension, :temperature, :max_tokens, :enabled, :default_config, :remark)
                """, params(id, request)
                .addValue("api_key_value", blankToNull(request.apiKey()))
                .addValue("api_key_masked", mask(request.apiKey()))
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("default_config", defaultConfig));
        return get(id);
    }

    public Map<String, Object> update(UUID id, ModelConfigRequest request) {
        get(id);
        validate(request);
        boolean defaultConfig = Boolean.TRUE.equals(request.defaultConfig());
        if (defaultConfig) {
            clearDefault(request.modelType());
        }
        // API Key 为空时保留旧值，避免编辑普通字段时误清密钥。
        jdbcTemplate.update("""
                UPDATE ai_model_config
                SET config_name = :config_name,
                    provider = :provider,
                    model_type = :model_type,
                    base_url = :base_url,
                    api_key_value = CASE WHEN :api_key_value IS NULL THEN api_key_value ELSE :api_key_value END,
                    api_key_masked = CASE WHEN :api_key_value IS NULL THEN api_key_masked ELSE :api_key_masked END,
                    model_name = :model_name,
                    dimension = :dimension,
                    temperature = :temperature,
                    max_tokens = :max_tokens,
                    enabled = :enabled,
                    default_config = :default_config,
                    remark = :remark,
                    updated_at = now()
                WHERE id = :id AND deleted = FALSE
                """, params(id, request)
                .addValue("api_key_value", blankToNull(request.apiKey()))
                .addValue("api_key_masked", mask(request.apiKey()))
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("default_config", defaultConfig));
        return get(id);
    }

    public void delete(UUID id) {
        int updated = jdbcTemplate.update("""
                UPDATE ai_model_config
                SET enabled = FALSE, default_config = FALSE, deleted = TRUE, updated_at = now()
                WHERE id = :id AND deleted = FALSE
                """, new MapSqlParameterSource("id", id));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在");
        }
    }

    public Map<String, Object> setDefault(UUID id) {
        Map<String, Object> current = getRaw(id);
        String modelType = String.valueOf(current.get("model_type"));
        clearDefault(modelType);
        jdbcTemplate.update("""
                UPDATE ai_model_config
                SET default_config = TRUE, enabled = TRUE, updated_at = now()
                WHERE id = :id AND deleted = FALSE
                """, new MapSqlParameterSource("id", id));
        return get(id);
    }

    public Map<String, Object> test(UUID id) {
        return runtimeService.testConfig(getRaw(id));
    }

    private Map<String, Object> getRaw(UUID id) {
        return jdbcTemplate.query("""
                        SELECT *
                        FROM ai_model_config
                        WHERE id = :id AND deleted = FALSE AND provider <> 'mock'
                        """, new MapSqlParameterSource("id", id), new ColumnMapRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在"));
    }

    private void clearDefault(String modelType) {
        jdbcTemplate.update("""
                UPDATE ai_model_config
                SET default_config = FALSE, updated_at = now()
                WHERE model_type = :model_type AND deleted = FALSE
                """, new MapSqlParameterSource("model_type", modelType));
    }

    private MapSqlParameterSource params(UUID id, ModelConfigRequest request) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("config_name", request.configName())
                .addValue("provider", request.provider())
                .addValue("model_type", request.modelType())
                .addValue("base_url", blankToNull(request.baseUrl()))
                .addValue("model_name", blankToNull(request.modelName()))
                .addValue("dimension", request.dimension())
                .addValue("temperature", request.temperature())
                .addValue("max_tokens", request.maxTokens())
                .addValue("remark", blankToNull(request.remark()));
    }

    private void validate(ModelConfigRequest request) {
        if (request.configName() == null || request.configName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "config_name 不能为空");
        }
        if (!PROVIDERS.contains(request.provider())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "provider 不支持");
        }
        if (!MODEL_TYPES.contains(request.modelType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "model_type 不支持");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }
}
