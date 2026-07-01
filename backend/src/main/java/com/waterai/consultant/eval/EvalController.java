package com.waterai.consultant.eval;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "问答评测")
@RestController
public class EvalController {

    private final EvalService evalService;
    private final TraceIdProvider traceIdProvider;

    public EvalController(EvalService evalService, TraceIdProvider traceIdProvider) {
        this.evalService = evalService;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "评测用例列表")
    @GetMapping("/api/eval-cases")
    public ApiResponse<List<Map<String, Object>>> listCases(@RequestParam(name = "project_id", required = false) UUID projectId) {
        return ok(evalService.listCases(projectId));
    }

    @Operation(summary = "新增评测用例")
    @PostMapping("/api/eval-cases")
    public ApiResponse<Map<String, Object>> createCase(@RequestBody EvalCaseRequest request) {
        return ok(evalService.createCase(request));
    }

    @Operation(summary = "更新评测用例")
    @PutMapping("/api/eval-cases/{id}")
    public ApiResponse<Map<String, Object>> updateCase(@PathVariable UUID id, @RequestBody EvalCaseRequest request) {
        return ok(evalService.updateCase(id, request));
    }

    @Operation(summary = "删除评测用例")
    @DeleteMapping("/api/eval-cases/{id}")
    public ApiResponse<Void> deleteCase(@PathVariable UUID id) {
        evalService.deleteCase(id);
        return ok(null);
    }

    @Operation(summary = "运行单条评测")
    @PostMapping("/api/eval-cases/{id}/run")
    public ApiResponse<Map<String, Object>> runCase(@PathVariable UUID id) {
        return ok(evalService.runCase(id));
    }

    @Operation(summary = "批量运行评测")
    @PostMapping("/api/eval-cases/run-batch")
    public ApiResponse<Map<String, Object>> runBatch(@RequestBody(required = false) EvalRunBatchRequest request) {
        return ok(evalService.runBatch(request));
    }

    @Operation(summary = "人工复核评测结果")
    @PutMapping("/api/eval-results/{id}/review")
    public ApiResponse<Map<String, Object>> reviewResult(@PathVariable UUID id, @RequestBody EvalReviewRequest request) {
        return ok(evalService.reviewResult(id, request));
    }

    @Operation(summary = "评测运行列表")
    @GetMapping("/api/eval-runs")
    public ApiResponse<List<Map<String, Object>>> listRuns(@RequestParam(name = "project_id", required = false) UUID projectId,
                                                           @RequestParam(name = "status", required = false) String status) {
        return ok(evalService.listRuns(projectId, status));
    }

    @Operation(summary = "评测运行详情")
    @GetMapping("/api/eval-runs/{id}")
    public ApiResponse<Map<String, Object>> getRun(@PathVariable UUID id) {
        return ok(evalService.getRun(id));
    }

    @Operation(summary = "评测运行结果")
    @GetMapping("/api/eval-runs/{id}/results")
    public ApiResponse<List<Map<String, Object>>> listRunResults(@PathVariable UUID id) {
        return ok(evalService.listRunResults(id));
    }

    @Operation(summary = "重跑失败用例")
    @PostMapping("/api/eval-runs/{id}/rerun-failed")
    public ApiResponse<Map<String, Object>> rerunFailed(@PathVariable UUID id) {
        return ok(evalService.rerunFailed(id));
    }

    @Operation(summary = "评测运行汇总")
    @GetMapping("/api/eval-runs/{id}/summary")
    public ApiResponse<Map<String, Object>> runSummary(@PathVariable UUID id) {
        return ok(evalService.runSummary(id));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, traceIdProvider.currentTraceId());
    }
}
