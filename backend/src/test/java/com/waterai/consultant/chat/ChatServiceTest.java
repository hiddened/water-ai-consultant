package com.waterai.consultant.chat;

import com.waterai.consultant.common.trace.TraceIdProvider;
import com.waterai.consultant.model.ModelInvocationResult;
import com.waterai.consultant.model.ModelRuntimeService;
import com.waterai.consultant.prompt.PromptRenderResult;
import com.waterai.consultant.prompt.PromptTemplateService;
import com.waterai.consultant.retrieval.KnowledgeEvidence;
import com.waterai.consultant.retrieval.KnowledgeRetrievalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private KnowledgeRetrievalService retrievalService;
    @Mock
    private PromptTemplateService promptTemplateService;
    @Mock
    private ModelRuntimeService modelRuntimeService;
    @Mock
    private ChatPersistenceService persistenceService;
    @Mock
    private TraceIdProvider traceIdProvider;
    @InjectMocks
    private ChatService chatService;

    @Test
    void shouldUseSpringAiResultAndKeepReferences() {
        UUID projectId = UUID.randomUUID();
        KnowledgeEvidence evidence = new KnowledgeEvidence(
                "CAPABILITY", UUID.randomUUID(), "告警自动建单", "configurable",
                "告警中心", "命中告警规则后可以自动创建维修工单", BigDecimal.valueOf(8.5)
        );
        when(retrievalService.retrieve(projectId, "告警后如何自动建单？", "business_qa", 8))
                .thenReturn(List.of(evidence));
        when(promptTemplateService.renderChat(projectId, "business_qa", "告警后如何自动建单？", List.of(evidence)))
                .thenReturn(new PromptRenderResult(null, "业务问答", "系统提示", "用户提示", "markdown"));
        when(modelRuntimeService.answer("系统提示", "用户提示", "告警后如何自动建单？", "business_qa", List.of(evidence)))
                .thenReturn(new ModelInvocationResult(null, "deepseek", "deepseek-chat", "可复用告警自动建单能力。"));
        when(traceIdProvider.currentTraceId()).thenReturn("trace-spring-ai");
        when(persistenceService.saveConversation(
                any(), anyString(), anyString(), anyString(), any(), anyString(), anyString(),
                nullable(UUID.class), anyString(), anyString(), nullable(UUID.class), anyString(),
                anyString(), anyString(), anyBoolean(), anyList()))
                .thenReturn(new ConversationSaveResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        ChatResponse response = chatService.chat(new ChatRequest(projectId, "business_qa", "告警后如何自动建单？"));

        assertThat(response.answer()).isEqualTo("可复用告警自动建单能力。");
        assertThat(response.references()).hasSize(1);
        assertThat(response.modelProvider()).isEqualTo("deepseek");
        assertThat(response.insufficientAnswer()).isFalse();
    }

    @Test
    void shouldRejectWithoutEvidenceAndSkipModelInvocation() {
        UUID projectId = UUID.randomUUID();
        when(retrievalService.retrieve(projectId, "是否支持区块链存证？", "business_qa", 8)).thenReturn(List.of());
        when(promptTemplateService.renderChat(projectId, "business_qa", "是否支持区块链存证？", List.of()))
                .thenReturn(new PromptRenderResult(null, "业务问答", "系统提示", "用户提示", "markdown"));
        when(modelRuntimeService.chatMetadata())
                .thenReturn(new com.waterai.consultant.model.ModelMetadata(null, "deepseek", "deepseek-chat"));
        when(traceIdProvider.currentTraceId()).thenReturn("trace-reject");
        when(persistenceService.saveConversation(
                any(), anyString(), anyString(), anyString(), any(), anyString(), anyString(),
                nullable(UUID.class), anyString(), anyString(), nullable(UUID.class), anyString(),
                anyString(), anyString(), anyBoolean(), anyList()))
                .thenReturn(new ConversationSaveResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        ChatResponse response = chatService.chat(new ChatRequest(projectId, "business_qa", "是否支持区块链存证？"));

        assertThat(response.answer()).isEqualTo(ChatService.INSUFFICIENT_ANSWER);
        assertThat(response.references()).isEmpty();
        assertThat(response.insufficientAnswer()).isTrue();
        verify(modelRuntimeService, never()).answer(anyString(), anyString(), anyString(), anyString(), anyList());
    }
}
