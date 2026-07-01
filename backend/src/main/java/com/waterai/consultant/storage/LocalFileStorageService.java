package com.waterai.consultant.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path storageRoot;

    public LocalFileStorageService(@Value("${app.storage.local-root}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(UUID projectId, UUID documentId, String fileName, MultipartFile file) throws Exception {
        String objectKey = "projects/" + projectId + "/" + documentId + "-" + fileName;
        Path target = storageRoot.resolve(objectKey).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        Files.createDirectories(target.getParent());
        file.transferTo(target);

        String filePath = "storage/" + objectKey;
        try (InputStream inputStream = Files.newInputStream(target)) {
            return new StoredFile("local", null, objectKey, filePath, StorageSupport.sha256(inputStream),
                    file.getSize(), file.getContentType());
        }
    }

    @Override
    public StoredFileResource open(Map<String, Object> document) throws Exception {
        Path filePath = resolveFilePath(StorageSupport.value(document.get("file_path")),
                StorageSupport.value(document.get("storage_object_key")));
        String fileName = StorageSupport.value(document.get("document_name"));
        String contentType = StorageSupport.value(document.get("content_type"));
        return new StoredFileResource(Files.newInputStream(filePath), fileName,
                contentType.isBlank() ? "application/octet-stream" : contentType, Files.size(filePath));
    }

    @Override
    public String storageType() {
        return "local";
    }

    private Path resolveFilePath(String filePath, String objectKey) {
        // 兼容历史数据：旧记录保存的是 storage/...，新记录额外保存 object_key。
        String location = !objectKey.isBlank() ? objectKey : filePath;
        Path path = Path.of(location);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        if (location.startsWith("storage/")) {
            return Path.of(location).toAbsolutePath().normalize();
        }
        return storageRoot.resolve(location).normalize();
    }
}
