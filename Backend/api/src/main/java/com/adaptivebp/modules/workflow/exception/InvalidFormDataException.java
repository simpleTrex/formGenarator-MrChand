package com.adaptivebp.modules.workflow.exception;

import java.util.Collections;
import java.util.Map;

public class InvalidFormDataException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public InvalidFormDataException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
