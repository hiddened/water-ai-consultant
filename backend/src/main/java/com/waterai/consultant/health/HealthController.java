package com.waterai.consultant.health;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.trace.TraceIdProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@Tag(name = "健康检查")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final TraceIdProvider traceIdProvider;

    public HealthController(TraceIdProvider traceIdProvider) {
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "服务健康检查")
    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse("UP", "water-ai-consultant", OffsetDateTime.now()),
                traceIdProvider.currentTraceId());
    }
}

