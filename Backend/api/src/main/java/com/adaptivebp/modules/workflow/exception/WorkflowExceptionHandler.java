package com.adaptivebp.modules.workflow.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.adaptivebp.modules.workflow")
public class WorkflowExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(WorkflowNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "WORKFLOW_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(WorkflowValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(WorkflowValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, "WORKFLOW_VALIDATION_FAILED", ex.getMessage(), ex.getErrors());
    }

    @ExceptionHandler(EdgeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEdgeNotFound(EdgeNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "EDGE_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(InsufficientEdgePermissionException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(InsufficientEdgePermissionException ex) {
        return error(HttpStatus.FORBIDDEN, "INSUFFICIENT_EDGE_PERMISSION", ex.getMessage(), null);
    }

    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<Map<String, Object>> handleCondition(ConditionNotMetException ex) {
        return error(HttpStatus.CONFLICT, "CONDITION_NOT_MET", ex.getMessage(), null);
    }

    @ExceptionHandler(WorkflowAlreadyCompletedException.class)
    public ResponseEntity<Map<String, Object>> handleCompleted(WorkflowAlreadyCompletedException ex) {
        return error(HttpStatus.CONFLICT, "WORKFLOW_ALREADY_COMPLETED", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidFormDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFormData(InvalidFormDataException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_FORM_DATA", ex.getMessage(), ex.getFieldErrors());
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMissingModel(ModelNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_STATE", ex.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String error, String message, Object details) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("details", details);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
