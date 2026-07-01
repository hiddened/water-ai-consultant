package com.waterai.consultant.document;

import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import com.waterai.consultant.storage.FileStorageService;
import com.waterai.consultant.storage.StoredFile;
import com.waterai.consultant.storage.StoredFileResource;
import com.waterai.consultant.task.TaskProgressReporter;
import com.waterai.consultant.vector.VectorStore;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DocumentTextExtractor textExtractor;
    private final DocumentChunker chunker;
    private final VectorStore vectorStore;
    private final FileStorageService fileStorageService;

    public DocumentService(NamedParameterJdbcTemplate jdbcTemplate,
                           DocumentTextExtractor textExtractor,
                           DocumentChunker chunker,
                           VectorStore vectorStore,
                           FileStorageService fileStorageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.textExtractor = textExtractor;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
        this.fileStorageService = fileStorageService;
    }

    public DocumentUploadResponse upload(UUID projectId,
                                         String moduleName,
                                         String documentType,
                                         MultipartFile file) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "project_id 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }

        UUID documentId = UUID.randomUUID();
        String originalName = sanitizeFileName(file.getOriginalFilename());

        try {
            StoredFile storedFile = fileStorageService.store(projectId, documentId, originalName, file);
            insertDocument(documentId, projectId, originalName, moduleName, documentType, storedFile);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档保存失败：" + ex.getMessage());
        }

        return new DocumentUploadResponse(getDocument(documentId), 0, null);
    }

    public int parseDocumentNow(UUID documentId, TaskProgressReporter reporter) {
        Map<String, Object> document = getDocument(documentId);
        updateParseStatus(documentId, "parsing", null);
        try {
            reporter.progress(10, "开始提取文档文本");
            StoredFileResource fileResource = fileStorageService.open(document);
            String text = textExtractor.extract(fileResource.inputStream(), fileResource.fileName());

            reporter.progress(45, "文本提取完成，开始切片");
            List<DocumentChunk> chunks = chunker.split(text);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("未提取到有效文本");
            }

            reporter.progress(65, "切片完成，开始写入数据库");
            replaceChunks(document, chunks);

            reporter.progress(80, "切片入库完成，开始生成索引");
            tryIndexDocument(documentId, reporter);
            updateParseStatus(documentId, "ready", null);
            reporter.progress(95, "文档解析和索引处理完成");
            return chunks.size();
        } catch (Exception ex) {
            // 解析失败只标记文档状态，不删除原始文件，方便后续调整解析器后重试。
            updateParseStatus(documentId, "failed", ex.getMessage());
            reporter.log("error", "文档解析失败：" + ex.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档解析失败：" + ex.getMessage());
        }
    }

    public int reindexDocumentNow(UUID documentId, TaskProgressReporter reporter) {
        Map<String, Object> document = getDocument(documentId);
        reporter.log("info", "准备重建索引：" + document.get("document_name"));
        reporter.progress(20, "开始生成文档切片 embedding");
        int chunkCount = vectorStore.indexDocument(documentId);
        reporter.progress(90, "索引写入完成");
        return chunkCount;
    }

    public int reindexAllNow(TaskProgressReporter reporter) {
        List<Map<String, Object>> documents = jdbcTemplate.queryForList("""
                SELECT id, document_name FROM ai_document
                WHERE deleted = FALSE AND enabled = TRUE AND parse_status = 'ready'
                ORDER BY updated_at DESC
                """, new MapSqlParameterSource());
        if (documents.isEmpty()) {
            reporter.progress(90, "没有需要重建索引的文档");
            return 0;
        }

        int totalChunks = 0;
        for (int i = 0; i < documents.size(); i++) {
            Map<String, Object> document = documents.get(i);
            UUID documentId = UUID.fromString(String.valueOf(document.get("id")));
            int progress = 10 + (int) (((double) i / documents.size()) * 80);
            reporter.progress(progress, "正在重建索引：" + document.get("document_name"));
            totalChunks += vectorStore.indexDocument(documentId);
        }
        reporter.progress(95, "批量索引重建完成");
        return totalChunks;
    }

    public List<DocumentChunkDto> listChunks(UUID documentId) {
        String sql = """
                SELECT id, document_id, document_title, COALESCE(chunk_index, chunk_no) AS chunk_index,
                       COALESCE(page_number, page_no) AS page_number, section_title, content, source_locator,
                       index_status, index_error, created_at
                FROM ai_document_chunk
                WHERE document_id = :document_id AND deleted = FALSE
                ORDER BY COALESCE(chunk_index, chunk_no)
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("document_id", documentId), (rs, rowNum) -> new DocumentChunkDto(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("document_id")),
                rs.getString("document_title"),
                rs.getInt("chunk_index"),
                (Integer) rs.getObject("page_number"),
                rs.getString("section_title"),
                rs.getString("content"),
                rs.getString("source_locator"),
                rs.getString("index_status"),
                rs.getString("index_error"),
                rs.getObject("created_at", OffsetDateTime.class)
        ));
    }

    public StoredFileResource openOriginalFile(UUID documentId) {
        try {
            return fileStorageService.open(getDocument(documentId));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档原文件读取失败：" + ex.getMessage());
        }
    }

    private void insertDocument(UUID documentId,
                                UUID projectId,
                                String documentName,
                                String moduleName,
                                String documentType,
                                StoredFile storedFile) {
        String sql = """
                INSERT INTO ai_document(id, project_id, document_name, module_name, document_type, file_path, file_hash, file_size,
                                        storage_type, storage_bucket, storage_object_key, content_type, parse_status, enabled)
                VALUES (:id, :project_id, :document_name, :module_name, :document_type, :file_path, :file_hash, :file_size,
                        :storage_type, :storage_bucket, :storage_object_key, :content_type, 'uploaded', TRUE)
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", documentId)
                .addValue("project_id", projectId)
                .addValue("document_name", documentName)
                .addValue("module_name", blankToNull(moduleName))
                .addValue("document_type", blankToNull(documentType))
                .addValue("file_path", storedFile.filePath())
                .addValue("file_hash", storedFile.fileHash())
                .addValue("file_size", storedFile.fileSize())
                .addValue("storage_type", storedFile.storageType())
                .addValue("storage_bucket", storedFile.bucket())
                .addValue("storage_object_key", storedFile.objectKey())
                .addValue("content_type", storedFile.contentType()));
    }

    private void replaceChunks(Map<String, Object> document, List<DocumentChunk> chunks) {
        UUID projectId = UUID.fromString(String.valueOf(document.get("project_id")));
        UUID documentId = UUID.fromString(String.valueOf(document.get("id")));
        String documentTitle = String.valueOf(document.get("document_name"));
        String filePath = String.valueOf(document.get("file_path"));

        // 重新解析会完整重建切片；物理删除旧切片可避免 document_id + chunk_no 唯一约束冲突。
        jdbcTemplate.update("""
                DELETE FROM ai_document_chunk
                WHERE document_id = :document_id
                """, new MapSqlParameterSource("document_id", documentId));

        String sql = """
                INSERT INTO ai_document_chunk(project_id, document_id, document_title, chunk_index, chunk_no,
                                              content, content_hash, page_number, page_no, section_title, source_locator, keywords, index_status)
                VALUES (:project_id, :document_id, :document_title, :chunk_index, :chunk_no,
                        :content, :content_hash, :page_number, :page_no, :section_title, :source_locator, :keywords, 'pending')
                """;
        for (DocumentChunk chunk : chunks) {
            int index = chunk.chunkIndex();
            String locator = filePath + "#chunk-" + index;
            jdbcTemplate.update(sql, new MapSqlParameterSource()
                    .addValue("project_id", projectId)
                    .addValue("document_id", documentId)
                    .addValue("document_title", documentTitle)
                    .addValue("chunk_index", index)
                    .addValue("chunk_no", index)
                    .addValue("content", chunk.content())
                    .addValue("content_hash", sha256(chunk.content()))
                    .addValue("page_number", chunk.pageNumber())
                    .addValue("page_no", chunk.pageNumber())
                    .addValue("section_title", chunk.sectionTitle())
                    .addValue("source_locator", locator)
                    .addValue("keywords", chunk.sectionTitle()));
        }
        refreshDocumentChunkCount(documentId);
    }

    private void tryIndexDocument(UUID documentId, TaskProgressReporter reporter) {
        try {
            int indexed = vectorStore.indexDocument(documentId);
            reporter.log("info", "索引处理完成，成功切片数：" + indexed);
        } catch (Exception ex) {
            jdbcTemplate.update("""
                    UPDATE ai_document
                    SET index_status = 'failed', index_error = :error, updated_at = now()
                    WHERE id = :id
                    """, new MapSqlParameterSource()
                    .addValue("id", documentId)
                    .addValue("error", ex.getMessage()));
            // 索引失败不回滚切片入库，让关键词检索仍可使用，任务日志保留失败原因。
            reporter.log("warn", "索引生成失败，已保留切片并降级关键词检索：" + ex.getMessage());
        }
    }

    private void refreshDocumentChunkCount(UUID documentId) {
        jdbcTemplate.update("""
                UPDATE ai_document d
                SET chunk_count = (
                    SELECT COUNT(*) FROM ai_document_chunk c WHERE c.document_id = d.id AND c.deleted = FALSE
                ),
                index_status = 'pending',
                index_error = NULL,
                last_indexed_at = NULL,
                updated_at = now()
                WHERE d.id = :id
                """, new MapSqlParameterSource("id", documentId));
    }

    private Map<String, Object> getDocument(UUID documentId) {
        String sql = "SELECT * FROM ai_document WHERE id = :id AND deleted = FALSE";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("id", documentId), new ColumnMapRowMapper())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
    }

    private void updateParseStatus(UUID documentId, String status, String error) {
        String sql = """
                UPDATE ai_document
                SET parse_status = :status,
                    parse_error = :error,
                    last_parsed_at = CASE WHEN :status = 'ready' THEN now() ELSE last_parsed_at END,
                    updated_at = now()
                WHERE id = :id AND deleted = FALSE
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", documentId)
                .addValue("status", status)
                .addValue("error", error));
    }

    private String sanitizeFileName(String name) {
        String safe = name == null || name.isBlank() ? "document.txt" : name;
        return safe.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes()));
        } catch (Exception ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
