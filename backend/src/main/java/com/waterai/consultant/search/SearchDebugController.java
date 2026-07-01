package com.waterai.consultant.search;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.trace.TraceIdProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "检索调试")
@RestController
@RequestMapping("/api/search")
public class SearchDebugController {

    private final SearchDebugService searchDebugService;
    private final TraceIdProvider traceIdProvider;

    public SearchDebugController(SearchDebugService searchDebugService, TraceIdProvider traceIdProvider) {
        this.searchDebugService = searchDebugService;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "检索调试，不调用大模型")
    @PostMapping("/debug")
    public ApiResponse<SearchDebugResponse> debug(@Valid @RequestBody SearchDebugRequest request) {
        return ApiResponse.success(searchDebugService.debug(request), traceIdProvider.currentTraceId());
    }
}
