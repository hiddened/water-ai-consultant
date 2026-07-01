package com.waterai.consultant.task;

import com.waterai.consultant.common.error.BusinessException;
import com.waterai.consultant.common.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class DocumentTaskService {

    private final TaskService taskService;
    private final DocumentTaskRunner runner;

    public DocumentTaskService(TaskService taskService, DocumentTaskRunner runner) {
        this.taskService = taskService;
        this.runner = runner;
    }

    public TaskResponse submitParse(UUID documentId, boolean initialParse) {
        String type = initialParse ? "document_parse" : "document_reparse";
        UUID taskId = taskService.create(type, documentId, "ai_document", "文档解析任务已创建");
        runner.runParse(taskId, documentId);
        return new TaskResponse(taskId, "pending", "文档解析任务已创建");
    }

    public TaskResponse submitReindex(UUID documentId) {
        UUID taskId = taskService.create("document_reindex", documentId, "ai_document", "文档索引任务已创建");
        runner.runReindex(taskId, documentId);
        return new TaskResponse(taskId, "pending", "文档索引任务已创建");
    }

    public TaskResponse submitReindexAll() {
        UUID taskId = taskService.create("document_reindex_all", null, "ai_document", "批量索引任务已创建");
        runner.runReindexAll(taskId);
        return new TaskResponse(taskId, "pending", "批量索引任务已创建");
    }

    public TaskResponse retry(UUID failedTaskId) {
        Map<String, Object> task = taskService.get(failedTaskId);
        if (!"failed".equals(String.valueOf(task.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有 failed 状态任务可以重试");
        }
        String taskType = String.valueOf(task.get("task_type"));
        Object bizIdValue = task.get("biz_id");
        if (bizIdValue == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该任务缺少业务对象，暂不支持重试");
        }
        UUID bizId = UUID.fromString(String.valueOf(bizIdValue));
        if ("document_parse".equals(taskType) || "document_reparse".equals(taskType)) {
            return submitParse(bizId, "document_parse".equals(taskType));
        }
        if ("document_index".equals(taskType) || "document_reindex".equals(taskType)) {
            return submitReindex(bizId);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "该任务类型暂不支持重试");
    }
}
