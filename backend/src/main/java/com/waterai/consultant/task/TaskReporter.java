package com.waterai.consultant.task;

import java.util.UUID;

public class TaskReporter implements TaskProgressReporter {

    private final TaskService taskService;
    private final UUID taskId;

    public TaskReporter(TaskService taskService, UUID taskId) {
        this.taskService = taskService;
        this.taskId = taskId;
    }

    @Override
    public void progress(int progress, String message) {
        taskService.progress(taskId, progress, message);
    }

    @Override
    public void log(String level, String message) {
        taskService.log(taskId, level, message);
    }
}
