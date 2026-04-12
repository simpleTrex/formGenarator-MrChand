package com.adaptivebp.modules.workflow.exception;

public class ModelNotFoundException extends RuntimeException {
    public ModelNotFoundException(String modelId) {
        super("DomainModel not found: " + modelId);
    }
}
