package com.sentinel.admin.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundMapsTo404() {
        ProblemDetail problem = handler.handleNotFound(new ResourceNotFoundException("Tenant not found: 1"));

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
    }

    @Test
    void illegalArgumentMapsTo400WithoutLeakingMessage() {
        ProblemDetail problem = handler.handleBadRequest(new IllegalArgumentException("secret detail"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertFalse(String.valueOf(problem.getDetail()).contains("secret"));
    }

    @Test
    void unexpectedErrorMapsTo500WithoutLeakingMessage() {
        ProblemDetail problem = handler.handleUnexpected(new RuntimeException("db password is x"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertFalse(String.valueOf(problem.getDetail()).contains("password"));
    }
}
