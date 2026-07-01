package com.waterai.consultant.storage;

public record StoredFile(
        String storageType,
        String bucket,
        String objectKey,
        String filePath,
        String fileHash,
        long fileSize,
        String contentType
) {
}
