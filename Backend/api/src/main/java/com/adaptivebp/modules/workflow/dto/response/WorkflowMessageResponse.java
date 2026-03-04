package com.adaptivebp.modules.workflow.dto.response;

/**
 * Simple message response DTO for the workflow module.
 */
public class WorkflowMessageResponse {
    private String message;

    public WorkflowMessageResponse() {}

    public WorkflowMessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
