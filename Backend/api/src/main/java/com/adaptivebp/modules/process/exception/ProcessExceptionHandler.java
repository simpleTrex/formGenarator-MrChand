package com.adaptivebp.modules.process.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.adaptivebp.modules.process")
public class ProcessExceptionHandler {

    @ExceptionHandler(ProcessNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ProcessNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProcessValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ProcessValidationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        body.put("errors", ex.getErrors());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(InvalidNodeSubmissionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSubmission(InvalidNodeSubmissionException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ProcessAlreadyCompletedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyCompleted(ProcessAlreadyCompletedException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InsufficientProcessPermissionException.class)
    public ResponseEntity<Map<String, Object>> handlePermission(InsufficientProcessPermissionException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
