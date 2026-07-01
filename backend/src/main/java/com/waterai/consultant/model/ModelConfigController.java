package com.waterai.consultant.model;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.trace.TraceIdProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "模型配置管理")
@RestController
@RequestMapping("/api/model-configs")
public class ModelConfigController {

    private final ModelConfigService service;
    private final TraceIdProvider traceIdProvider;

    public ModelConfigController(ModelConfigService service, TraceIdProvider traceIdProvider) {
        this.service = service;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "查询模型配置列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String provider,
                                                       @RequestParam(name = "model_type", required = false) String modelType,
                                                       @RequestParam(required = false) Boolean enabled) {
        return ok(service.list(provider, modelType, enabled));
    }

    @Operation(summary = "查询模型配置详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable UUID id) {
        return ok(service.get(id));
    }

    @Operation(summary = "新增模型配置")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody ModelConfigRequest request) {
        return ok(service.create(request));
    }

    @Operation(summary = "更新模型配置")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable UUID id, @RequestBody ModelConfigRequest request) {
        return ok(service.update(id, request));
    }

    @Operation(summary = "删除模型配置")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ok(null);
    }

    @Operation(summary = "测试模型连接")
    @PostMapping("/{id}/test")
    public ApiResponse<Map<String, Object>> test(@PathVariable UUID id) {
        return ok(service.test(id));
    }

    @Operation(summary = "设置默认模型配置")
    @PostMapping("/{id}/set-default")
    public ApiResponse<Map<String, Object>> setDefault(@PathVariable UUID id) {
        return ok(service.setDefault(id));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, traceIdProvider.currentTraceId());
    }
}
