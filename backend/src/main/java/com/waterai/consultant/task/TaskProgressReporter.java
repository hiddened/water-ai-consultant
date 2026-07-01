package com.waterai.consultant.task;

public interface TaskProgressReporter {

    void progress(int progress, String message);

    void log(String level, String message);
}
