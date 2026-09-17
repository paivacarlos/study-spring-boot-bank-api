package com.study.payments.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests: ResourceNotFoundException")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Should instantiate exception with message correctly")
    void shouldCreateExceptionWithMessage() {
        // ARRANGE & ACT
        String errorMessage = "Pix transaction not found with ID: 123";
        ResourceNotFoundException exception = new ResourceNotFoundException(errorMessage);

        // ASSERT
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause(), "Cause should be null when not provided");
    }

    @Test
    @DisplayName("Should instantiate exception with message and cause correctly")
    void shouldCreateExceptionWithMessageAndCause() {
        // ARRANGE
        String errorMessage = "Pix transaction lookup failed";
        Throwable rootCause = new IllegalArgumentException("Invalid UUID format");

        // ACT
        ResourceNotFoundException exception = new ResourceNotFoundException(errorMessage, rootCause);

        // ASSERT
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(rootCause, exception.getCause(), "Root cause should be preserved in the exception");
    }

    @Test
    @DisplayName("Should contain @ResponseStatus annotation mapped to HTTP 404 NOT_FOUND")
    void shouldHaveResponseStatusAnnotationWithNotFound() {
        // ARRANGE & ACT: Inspeciona os metadados da classe via Reflexão do Java
        ResponseStatus annotation = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);

        // ASSERT: Garante o contrato com o Spring MVC
        assertNotNull(annotation, "Class should have @ResponseStatus annotation");
        assertEquals(HttpStatus.NOT_FOUND, annotation.value(), "HTTP status configured should be 404 NOT_FOUND");
    }
}
