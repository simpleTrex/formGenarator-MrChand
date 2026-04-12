package com.adaptivebp.modules.process.exception;

public class ProcessAlreadyCompletedException extends RuntimeException {
    public ProcessAlreadyCompletedException(String instanceId) {
        super("Process instance '" + instanceId + "' is no longer active");
    }
}
