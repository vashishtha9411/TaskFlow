package com.taskflow.exception;

public class TaskFlowAccessDeniedException extends RuntimeException {
    public TaskFlowAccessDeniedException(String message) {
        super(message);
    }
}
