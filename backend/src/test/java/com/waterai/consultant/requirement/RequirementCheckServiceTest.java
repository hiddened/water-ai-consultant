package com.waterai.consultant.requirement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterai.consultant.chat.ChatPersistenceService;
import com.waterai.consultant.chat.ChatService;
import com.waterai.consultant.chat.ConversationSaveResult;
import com.waterai.consultant.common.trace.TraceIdProvider;
import com.waterai.consultant.model.ModelMetadata;
import com.waterai.consultant.model.ModelRuntimeService;
import com.waterai.consultant.prompt.PromptRenderResult;
import com.waterai.consultant.prompt.PromptTemplateService;
import com.waterai.consultant.retrieval.KnowledgeRetrievalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RequirementCheckServiceTest {

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

    @Test
    void shouldReturnLevelDWithoutCallingModelWhenEvidenceIsMissing() {
        UUID projectId = UUID.randomUUID();
        RequirementCheckService service = new RequirementCheckService(
                retrievalService, promptTemplateService, modelRuntimeService,
                persistenceService, traceIdProvider, new ObjectMapper());
        when(retrievalService.retrieve(projectId, "当前项目是否支持无人机巡检？ ", "requirement_check", 10))
                .thenReturn(List.of());
        when(promptTemplateService.renderRequirement(projectId, "当前项目是否支持无人机巡检？", null, List.of()))
                .thenReturn(new PromptRenderResult(null, "需求分析", "系统提示", "用户提示", "structured_json"));
        when(modelRuntimeService.chatMetadata()).thenReturn(new ModelMetadata(null, "deepseek", "deepseek-chat"));
        when(traceIdProvider.currentTraceId()).thenReturn("trace-requirement-reject");
        when(persistenceService.saveConversation(
                any(), anyString(), anyString(), anyString(), any(), anyString(), anyString(),
                nullable(UUID.class), anyString(), anyString(), nullable(UUID.class), anyString(),
                anyString(), anyString(), anyBoolean(), anyList()))
                .thenReturn(new ConversationSaveResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        RequirementCheckResponse response = service.check(
                new RequirementCheckRequest(projectId, "当前项目是否支持无人机巡检？", null));

        assertThat(response.feasibilityLevel()).isEqualTo("D");
        assertThat(response.conclusion()).isEqualTo(ChatService.INSUFFICIENT_ANSWER);
        assertThat(response.references()).isEmpty();
        assertThat(response.insufficientAnswer()).isTrue();
        verify(modelRuntimeService, never()).analyzeRequirement(
                anyString(), anyString(), anyString(), nullable(String.class), anyList());
    }
}
