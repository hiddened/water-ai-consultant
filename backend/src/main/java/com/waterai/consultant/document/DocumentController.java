package com.waterai.consultant.document;

import com.waterai.consultant.common.api.ApiResponse;
import com.waterai.consultant.common.trace.TraceIdProvider;
import com.waterai.consultant.storage.StoredFileResource;
import com.waterai.consultant.task.DocumentTaskService;
import com.waterai.consultant.task.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Tag(name = "文档上传解析")
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentTaskService documentTaskService;
    private final TraceIdProvider traceIdProvider;

    public DocumentController(DocumentService documentService,
                              DocumentTaskService documentTaskService,
                              TraceIdProvider traceIdProvider) {
        this.documentService = documentService;
        this.documentTaskService = documentTaskService;
        this.traceIdProvider = traceIdProvider;
    }

    @Operation(summary = "上传文档并创建解析任务")
    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResponse> upload(@RequestParam("project_id") UUID projectId,
                                                      @RequestParam(name = "module_name", required = false) String moduleName,
                                                      @RequestParam(name = "document_type", required = false) String documentType,
                                                      @RequestParam("file") MultipartFile file) {
        DocumentUploadResponse response = documentService.upload(projectId, moduleName, documentType, file);
        UUID documentId = UUID.fromString(String.valueOf(response.document().get("id")));
        TaskResponse task = documentTaskService.submitParse(documentId, true);
        return ok(new DocumentUploadResponse(response.document(), response.chunkCount(), task.taskId()));
    }

    @Operation(summary = "创建重新解析文档任务")
    @PostMapping("/{id}/parse")
    public ApiResponse<TaskResponse> parse(@PathVariable UUID id) {
        return ok(documentTaskService.submitParse(id, false));
    }

    @Operation(summary = "创建重建指定文档向量索引任务")
    @PostMapping("/{id}/reindex")
    public ApiResponse<TaskResponse> reindex(@PathVariable UUID id) {
        return ok(documentTaskService.submitReindex(id));
    }

    @Operation(summary = "创建重建全部已解析文档向量索引任务")
    @PostMapping("/reindex-all")
    public ApiResponse<TaskResponse> reindexAll() {
        return ok(documentTaskService.submitReindexAll());
    }

    @Operation(summary = "查看文档切片")
    @GetMapping("/{id}/chunks")
    public ApiResponse<List<DocumentChunkDto>> chunks(@PathVariable UUID id) {
        return ok(documentService.listChunks(id));
    }

    @Operation(summary = "下载文档原文件")
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        StoredFileResource file = documentService.openOriginalFile(id);
        String encodedFileName = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentLength(Math.max(file.contentLength(), 0))
                .body(new InputStreamResource(file.inputStream()));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, traceIdProvider.currentTraceId());
    }
}
