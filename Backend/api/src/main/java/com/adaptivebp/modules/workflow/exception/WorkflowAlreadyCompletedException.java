package com.adaptivebp.modules.workflow.exception;

public class WorkflowAlreadyCompletedException extends RuntimeException {
    public WorkflowAlreadyCompletedException(String message) {
        super(message);
    }
}
