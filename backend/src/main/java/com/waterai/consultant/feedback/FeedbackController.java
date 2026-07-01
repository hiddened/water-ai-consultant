package com.waterai.consultant.feedback;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.trace.TraceIdProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "反馈管理")
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService service;
    private final TraceIdProvider traceIdProvider;

    public FeedbackController(FeedbackService service, TraceIdProvider traceIdProvider) {
        this.service = service;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "查询反馈列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(name = "project_id", required = false) UUID projectId,
                                                       @RequestParam(name = "feedback_type", required = false) String feedbackType,
                                                       @RequestParam(name = "review_status", required = false) String reviewStatus,
                                                       @RequestParam(name = "keyword", required = false) String keyword,
                                                       @RequestParam(name = "start_time", required = false) String startTime,
                                                       @RequestParam(name = "end_time", required = false) String endTime) {
        return ok(service.list(projectId, feedbackType, reviewStatus, keyword, startTime, endTime));
    }

    @Operation(summary = "查询反馈详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable UUID id) {
        return ok(service.get(id));
    }

    @Operation(summary = "提交问答反馈")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody FeedbackRequest request) {
        return ok(service.create(request));
    }

    @Operation(summary = "复核反馈")
    @PutMapping("/{id}/review")
    public ApiResponse<Map<String, Object>> review(@PathVariable UUID id, @RequestBody FeedbackReviewRequest request) {
        return ok(service.review(id, request));
    }

    @Operation(summary = "反馈转知识库草稿")
    @PostMapping("/{id}/convert")
    public ApiResponse<Map<String, Object>> convert(@PathVariable UUID id) {
        return ok(service.convert(id));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, traceIdProvider.currentTraceId());
    }
}
