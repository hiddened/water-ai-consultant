package com.waterai.consultant.task;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import com.waterai.consultant.common.trace.TraceIdProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "任务中心")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final DocumentTaskService documentTaskService;
    private final TraceIdProvider traceIdProvider;

    public TaskController(TaskService taskService,
                          DocumentTaskService documentTaskService,
                          TraceIdProvider traceIdProvider) {
        this.taskService = taskService;
        this.documentTaskService = documentTaskService;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "查询任务列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(name = "task_type", required = false) String taskType,
                                                       @RequestParam(name = "status", required = false) String status,
                                                       @RequestParam(name = "biz_id", required = false) UUID bizId) {
        return ok(taskService.list(taskType, status, bizId));
    }

    @Operation(summary = "查询任务详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable UUID id) {
        return ok(taskService.get(id));
    }

    @Operation(summary = "查询任务日志")
    @GetMapping("/{id}/logs")
    public ApiResponse<List<Map<String, Object>>> logs(@PathVariable UUID id) {
        return ok(taskService.logs(id));
    }

    @Operation(summary = "重试失败任务")
    @PostMapping("/{id}/retry")
    public ApiResponse<TaskResponse> retry(@PathVariable UUID id) {
        return ok(documentTaskService.retry(id));
    }

    @Operation(summary = "取消待执行任务")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable UUID id) {
        if (!taskService.cancelPending(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前只支持取消 pending 状态任务");
        }
        return ok(taskService.get(id));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, traceIdProvider.currentTraceId());
    }
}
