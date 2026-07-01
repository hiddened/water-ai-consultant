package com.waterai.consultant.storage;

import java.io.InputStream;

public record StoredFileResource(
        InputStream inputStream,
        String fileName,
        String contentType,
        long contentLength
) {
}
