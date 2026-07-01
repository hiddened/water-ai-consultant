package com.waterai.consultant.chat;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.trace.TraceIdProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "对话记录")
@RestController
@RequestMapping("/api/chat-sessions")
public class ChatSessionController {

    private final ChatSessionService service;
    private final TraceIdProvider traceIdProvider;

    public ChatSessionController(ChatSessionService service, TraceIdProvider traceIdProvider) {
        this.service = service;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "查询对话会话列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(name = "project_id", required = false) UUID projectId,
                                                       @RequestParam(name = "mode", required = false) String mode,
                                                       @RequestParam(name = "keyword", required = false) String keyword,
                                                       @RequestParam(name = "start_time", required = false) String startTime,
                                                       @RequestParam(name = "end_time", required = false) String endTime) {
        return ok(service.list(projectId, mode, keyword, startTime, endTime));
    }

    @Operation(summary = "查询对话会话详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable UUID id) {
        return ok(service.get(id));
    }

    @Operation(summary = "查询对话消息")
    @GetMapping("/{id}/messages")
    public ApiResponse<List<Map<String, Object>>> messages(@PathVariable UUID id) {
        return ok(service.messages(id));
    }

    @Operation(summary = "删除对话会话")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ok(null);
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, traceIdProvider.currentTraceId());
    }
}
