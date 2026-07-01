package com.waterai.consultant.document;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Path;

@Component
public class DocumentTextExtractor {

    private final AutoDetectParser parser = new AutoDetectParser();

    public String extract(Path filePath) throws Exception {
        return extract(java.nio.file.Files.newInputStream(filePath), filePath.getFileName().toString());
    }

    public String extract(InputStream inputStream, String resourceName) throws Exception {
        try (InputStream closeable = inputStream) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, resourceName);
            parser.parse(closeable, handler, metadata, new ParseContext());
            return normalize(handler.toString());
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
