package com.waterai.consultant.chat;

import java.util.UUID;

public record ConversationSaveResult(
        UUID sessionId,
        UUID userMessageId,
        UUID assistantMessageId
) {
}
