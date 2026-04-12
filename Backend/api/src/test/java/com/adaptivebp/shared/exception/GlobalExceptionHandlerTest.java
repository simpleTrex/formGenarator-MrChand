package com.adaptivebp.shared.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResponseStatus_preservesStatusCodeAndReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Insufficient permissions", response.getBody().get("message"));
        assertEquals(false, response.getBody().get("success"));
    }

    @Test
    void handleGeneral_returnsInternalServerError() {
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }
}
