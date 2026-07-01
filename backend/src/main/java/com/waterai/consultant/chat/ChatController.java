package com.waterai.consultant.chat;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.trace.TraceIdProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 问答")
@RestController
public class ChatController {

    private final ChatService chatService;
    private final TraceIdProvider traceIdProvider;

    public ChatController(ChatService chatService, TraceIdProvider traceIdProvider) {
        this.chatService = chatService;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "智能问答")
    @PostMapping("/api/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.chat(request), traceIdProvider.currentTraceId());
    }
}

