package com.waterai.consultant.chat;

import java.util.UUID;

public record RelatedItem(
        UUID id,
        String title,
        String locator
) {
}

