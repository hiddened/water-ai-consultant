package com.waterai.consultant.requirement;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.trace.TraceIdProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "需求可行性分析")
@RestController
public class RequirementCheckController {

    private final RequirementCheckService service;
    private final TraceIdProvider traceIdProvider;

    public RequirementCheckController(RequirementCheckService service, TraceIdProvider traceIdProvider) {
        this.service = service;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "需求可行性分析")
    @PostMapping("/api/requirements/check")
    public ApiResponse<RequirementCheckResponse> check(@Valid @RequestBody RequirementCheckRequest request) {
        return ApiResponse.success(service.check(request), traceIdProvider.currentTraceId());
    }
}

