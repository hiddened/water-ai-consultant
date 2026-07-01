package com.waterai.consultant.task;

import com.waterai.consultant.document.DocumentService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DocumentTaskRunner {

    private final TaskService taskService;
    private final DocumentService documentService;

    public DocumentTaskRunner(TaskService taskService, DocumentService documentService) {
        this.taskService = taskService;
        this.documentService = documentService;
    }

    @Async
    public void runParse(UUID taskId, UUID documentId) {
        TaskReporter reporter = new TaskReporter(taskService, taskId);
        try {
            taskService.start(taskId, "开始解析文档");
            int chunkCount = documentService.parseDocumentNow(documentId, reporter);
            taskService.success(taskId, "文档解析完成，生成切片 " + chunkCount + " 条");
        } catch (Exception ex) {
            taskService.fail(taskId, "文档解析失败", ex);
        }
    }

    @Async
    public void runReindex(UUID taskId, UUID documentId) {
        TaskReporter reporter = new TaskReporter(taskService, taskId);
        try {
            taskService.start(taskId, "开始重建文档索引");
            int chunkCount = documentService.reindexDocumentNow(documentId, reporter);
            taskService.success(taskId, "文档索引重建完成，处理切片 " + chunkCount + " 条");
        } catch (Exception ex) {
            taskService.fail(taskId, "文档索引重建失败", ex);
        }
    }

    @Async
    public void runReindexAll(UUID taskId) {
        TaskReporter reporter = new TaskReporter(taskService, taskId);
        try {
            taskService.start(taskId, "开始批量重建文档索引");
            int chunkCount = documentService.reindexAllNow(reporter);
            taskService.success(taskId, "批量索引重建完成，处理切片 " + chunkCount + " 条");
        } catch (Exception ex) {
            taskService.fail(taskId, "批量索引重建失败", ex);
        }
    }
}
