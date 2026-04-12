package com.adaptivebp.modules.workflow.exception;

import java.util.ArrayList;
import java.util.List;

public class WorkflowValidationException extends RuntimeException {
    private final List<String> errors;

    public WorkflowValidationException(List<String> errors) {
        super("Workflow validation failed");
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public List<String> getErrors() {
        return errors;
    }
}
