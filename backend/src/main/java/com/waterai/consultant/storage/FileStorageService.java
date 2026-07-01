package com.waterai.consultant.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

public interface FileStorageService {

    StoredFile store(UUID projectId, UUID documentId, String fileName, MultipartFile file) throws Exception;

    StoredFileResource open(Map<String, Object> document) throws Exception;

    String storageType();
}
