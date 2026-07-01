package com.waterai.consultant.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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
@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final String bucket;
    private final Path storageRoot;

    public MinioFileStorageService(@Value("${app.storage.minio.endpoint}") String endpoint,
                                   @Value("${app.storage.minio.access-key}") String accessKey,
                                   @Value("${app.storage.minio.secret-key}") String secretKey,
                                   @Value("${app.storage.minio.bucket}") String bucket,
                                   @Value("${app.storage.local-root}") String storageRoot) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(UUID projectId, UUID documentId, String fileName, MultipartFile file) throws Exception {
        ensureBucket();
        String objectKey = "projects/" + projectId + "/" + documentId + "-" + fileName;
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();

        String fileHash;
        try (InputStream inputStream = file.getInputStream()) {
            fileHash = StorageSupport.sha256(inputStream);
        }

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        }

        return new StoredFile("minio", bucket, objectKey, "minio://" + bucket + "/" + objectKey,
                fileHash, file.getSize(), contentType);
    }

    @Override
    public StoredFileResource open(Map<String, Object> document) throws Exception {
        String storageType = StorageSupport.value(document.get("storage_type"));
        if (!storageType.isBlank() && !"minio".equalsIgnoreCase(storageType)) {
            return openLocalFallback(document);
        }

        String documentBucket = StorageSupport.value(document.get("storage_bucket"));
        String objectKey = StorageSupport.value(document.get("storage_object_key"));
        if (documentBucket.isBlank()) {
            documentBucket = bucket;
        }
        if (objectKey.isBlank()) {
            String filePath = StorageSupport.value(document.get("file_path"));
            objectKey = filePath.startsWith("minio://" + documentBucket + "/")
                    ? filePath.substring(("minio://" + documentBucket + "/").length())
                    : filePath;
        }

        InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(documentBucket)
                .object(objectKey)
                .build());
        String fileName = StorageSupport.value(document.get("document_name"));
        String contentType = StorageSupport.value(document.get("content_type"));
        long fileSize = parseLong(document.get("file_size"));
        return new StoredFileResource(inputStream, fileName,
                contentType.isBlank() ? "application/octet-stream" : contentType, fileSize);
    }

    @Override
    public String storageType() {
        return "minio";
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private long parseLong(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private StoredFileResource openLocalFallback(Map<String, Object> document) throws Exception {
        String filePath = StorageSupport.value(document.get("file_path"));
        String objectKey = StorageSupport.value(document.get("storage_object_key"));
        String location = !objectKey.isBlank() ? objectKey : filePath;
        Path path = Path.of(location);
        if (!path.isAbsolute()) {
            path = location.startsWith("storage/")
                    ? Path.of(location).toAbsolutePath().normalize()
                    : storageRoot.resolve(location).normalize();
        }
        String fileName = StorageSupport.value(document.get("document_name"));
        String contentType = StorageSupport.value(document.get("content_type"));
        return new StoredFileResource(Files.newInputStream(path), fileName,
                contentType.isBlank() ? "application/octet-stream" : contentType, Files.size(path));
    }
}
