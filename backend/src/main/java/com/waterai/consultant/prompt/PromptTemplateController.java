package com.waterai.consultant.prompt;

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

@Tag(name = "Prompt 模板管理")
@RestController
@RequestMapping("/api/prompt-templates")
public class PromptTemplateController {

    private final PromptTemplateService service;
    private final TraceIdProvider traceIdProvider;

    public PromptTemplateController(PromptTemplateService service, TraceIdProvider traceIdProvider) {
        this.service = service;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "查询 Prompt 模板列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(name = "template_type", required = false) String templateType,
                                                       @RequestParam(required = false) String mode,
                                                       @RequestParam(required = false) Boolean enabled) {
        return ok(service.list(templateType, mode, enabled));
    }

    @Operation(summary = "查询 Prompt 模板详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable UUID id) {
        return ok(service.get(id));
    }

    @Operation(summary = "新增 Prompt 模板")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody PromptTemplateRequest request) {
        return ok(service.create(request));
    }

    @Operation(summary = "更新 Prompt 模板")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable UUID id, @RequestBody PromptTemplateRequest request) {
        return ok(service.update(id, request));
    }

    @Operation(summary = "删除 Prompt 模板")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ok(null);
    }

    @Operation(summary = "测试 Prompt 模板渲染")
    @PostMapping("/{id}/test")
    public ApiResponse<Map<String, Object>> test(@PathVariable UUID id, @RequestBody PromptTemplateTestRequest request) {
        return ok(service.test(id, request));
    }

    @Operation(summary = "设置默认 Prompt 模板")
    @PostMapping("/{id}/set-default")
    public ApiResponse<Map<String, Object>> setDefault(@PathVariable UUID id) {
        return ok(service.setDefault(id));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, traceIdProvider.currentTraceId());
    }
}
