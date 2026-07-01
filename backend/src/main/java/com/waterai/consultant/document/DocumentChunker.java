package com.waterai.consultant.document;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {

    private static final int MAX_CHUNK_LENGTH = 900;
    private static final int OVERLAP_LENGTH = 120;

    public List<DocumentChunk> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        String currentSection = null;
        StringBuilder buffer = new StringBuilder();
        for (String paragraph : text.split("\\n\\s*\\n|\\n")) {
            String normalized = paragraph.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (isSectionTitle(normalized)) {
                currentSection = normalized.replaceFirst("^#+\\s*", "");
            }

            // 单段过长时直接切分，避免为了保留段落完整性导致上下文失控。
            if (normalized.length() > MAX_CHUNK_LENGTH) {
                flushBuffer(chunks, buffer, currentSection);
                splitLongParagraph(chunks, normalized, currentSection);
                continue;
            }

            if (buffer.length() + normalized.length() + 1 > MAX_CHUNK_LENGTH) {
                flushBuffer(chunks, buffer, currentSection);
            }
            if (!buffer.isEmpty()) {
                buffer.append('\n');
            }
            buffer.append(normalized);
        }
        flushBuffer(chunks, buffer, currentSection);
        return chunks;
    }

    private void splitLongParagraph(List<DocumentChunk> chunks, String paragraph, String sectionTitle) {
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, paragraph.length());
            addChunk(chunks, paragraph.substring(start, end), sectionTitle);
            if (end == paragraph.length()) {
                break;
            }
            start = Math.max(end - OVERLAP_LENGTH, start + 1);
        }
    }

    private void flushBuffer(List<DocumentChunk> chunks, StringBuilder buffer, String sectionTitle) {
        if (buffer.isEmpty()) {
            return;
        }
        addChunk(chunks, buffer.toString(), sectionTitle);
        String tail = buffer.length() > OVERLAP_LENGTH
                ? buffer.substring(buffer.length() - OVERLAP_LENGTH)
                : "";
        buffer.setLength(0);
        if (!tail.isBlank()) {
            buffer.append(tail.trim());
        }
    }

    private void addChunk(List<DocumentChunk> chunks, String content, String sectionTitle) {
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        chunks.add(new DocumentChunk(chunks.size(), null, sectionTitle, trimmed));
    }

    private boolean isSectionTitle(String paragraph) {
        if (paragraph.startsWith("#")) {
            return true;
        }
        return paragraph.length() <= 40
                && !paragraph.endsWith("。")
                && !paragraph.endsWith("；")
                && !paragraph.endsWith(".");
    }
}
