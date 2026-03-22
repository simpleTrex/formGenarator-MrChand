package com.adaptivebp.modules.process.exception;

import java.util.List;

public class ProcessValidationException extends RuntimeException {

    private final List<String> errors;

    public ProcessValidationException(List<String> errors) {
        super("Process definition validation failed");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
