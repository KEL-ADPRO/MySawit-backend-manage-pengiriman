package com.mysawit.pengiriman.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    @DisplayName("handleNotFound should return 404")
    void handleNotFound() {
        ResponseEntity<Map<String, Object>> response =
            handler.handleNotFound(new ResourceNotFoundException("not found"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertEquals("not found", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("handleBadRequest should return 400 for BusinessRuleViolationException")
    void handleBusinessRuleViolation() {
        ResponseEntity<Map<String, Object>> response =
            handler.handleBadRequest(new BusinessRuleViolationException("bad rule"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertEquals("bad rule", response.getBody().get("message"));
    }

    @Test
    @DisplayName("handleBadRequest should return 400 for IllegalArgumentException")
    void handleIllegalArgument() {
        ResponseEntity<Map<String, Object>> response =
            handler.handleBadRequest(new IllegalArgumentException("bad arg"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertEquals("bad arg", response.getBody().get("message"));
    }

    @Test
    @DisplayName("handleForbidden should return 403")
    void handleForbidden() {
        ResponseEntity<Map<String, Object>> response =
            handler.handleForbidden(new AccessDeniedBusinessException("denied"));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
        assertEquals("denied", response.getBody().get("message"));
    }
}
