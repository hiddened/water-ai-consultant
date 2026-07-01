package com.waterai.consultant.chat;

import com.waterai.consultant.retrieval.KnowledgeEvidence;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ChatPersistenceService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChatPersistenceService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ConversationSaveResult saveConversation(UUID projectId,
                                                   String mode,
                                                   String question,
                                                   String answer,
                                                   BigDecimal confidence,
                                                   String traceId,
                                                   String llmProvider,
                                                   UUID modelConfigId,
                                                   String modelProvider,
                                                   String modelName,
                                                   UUID promptTemplateId,
                                                   String promptTemplateName,
                                                   String promptRenderedPreview,
                                                   String searchStrategy,
                                                   boolean insufficientAnswer,
                                                   List<KnowledgeEvidence> evidences) {
        UUID sessionId = insertSession(projectId, mode, title(question));
        UUID userMessageId = insertMessage(sessionId, projectId, "user", question, null, traceId,
                null, null, null, null, null, null, null, null, false);
        UUID answerMessageId = insertMessage(sessionId, projectId, "assistant", answer, confidence, traceId,
                llmProvider, modelConfigId, modelProvider, modelName, promptTemplateId, promptTemplateName,
                promptRenderedPreview, searchStrategy, insufficientAnswer);
        insertReferences(answerMessageId, projectId, evidences);
        return new ConversationSaveResult(sessionId, userMessageId, answerMessageId);
    }

    private UUID insertSession(UUID projectId, String mode, String title) {
        String sql = """
                INSERT INTO ai_chat_session(project_id, title, mode)
                VALUES (:project_id, :title, :mode)
                RETURNING id
                """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("title", title)
                .addValue("mode", mode), UUID.class);
    }

    private UUID insertMessage(UUID sessionId,
                               UUID projectId,
                               String role,
                               String content,
                               BigDecimal confidence,
                               String traceId,
                               String llmProvider,
                               UUID modelConfigId,
                               String modelProvider,
                               String modelName,
                               UUID promptTemplateId,
                               String promptTemplateName,
                               String promptRenderedPreview,
                               String searchStrategy,
                               boolean insufficientAnswer) {
        String sql = """
                INSERT INTO ai_chat_message(session_id, project_id, role, content, confidence, trace_id,
                                            llm_provider, model_config_id, model_provider, model_name,
                                            prompt_template_id, prompt_template_name, prompt_rendered_preview,
                                            search_strategy, insufficient_answer)
                VALUES (:session_id, :project_id, :role, :content, :confidence, :trace_id,
                        :llm_provider, :model_config_id, :model_provider, :model_name,
                        :prompt_template_id, :prompt_template_name, :prompt_rendered_preview,
                        :search_strategy, :insufficient_answer)
                RETURNING id
                """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                .addValue("session_id", sessionId)
                .addValue("project_id", projectId)
                .addValue("role", role)
                .addValue("content", content)
                .addValue("confidence", confidence)
                .addValue("trace_id", traceId)
                .addValue("llm_provider", llmProvider)
                .addValue("model_config_id", modelConfigId)
                .addValue("model_provider", modelProvider)
                .addValue("model_name", modelName)
                .addValue("prompt_template_id", promptTemplateId)
                .addValue("prompt_template_name", promptTemplateName)
                .addValue("prompt_rendered_preview", truncate(promptRenderedPreview, 1200))
                .addValue("search_strategy", searchStrategy)
                .addValue("insufficient_answer", insufficientAnswer), UUID.class);
    }

    private void insertReferences(UUID messageId, UUID projectId, List<KnowledgeEvidence> evidences) {
        String sql = """
                INSERT INTO ai_answer_reference(message_id, project_id, source_type, source_id, source_title, source_locator, quote, score)
                VALUES (:message_id, :project_id, :source_type, :source_id, :source_title, :source_locator, :quote, :score)
                """;
        for (KnowledgeEvidence evidence : evidences) {
            jdbcTemplate.update(sql, new MapSqlParameterSource()
                    .addValue("message_id", messageId)
                    .addValue("project_id", projectId)
                    .addValue("source_type", evidence.sourceType())
                    .addValue("source_id", evidence.sourceId())
                    .addValue("source_title", evidence.sourceTitle())
                    .addValue("source_locator", evidence.sourceLocator())
                    .addValue("quote", truncate(evidence.content(), 500))
                    .addValue("score", evidence.score()));
        }
    }

    private String title(String question) {
        if (question == null || question.isBlank()) {
            return "新对话";
        }
        return question.length() > 80 ? question.substring(0, 80) : question;
    }

    private String truncate(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }
        return value.substring(0, length);
    }
}
