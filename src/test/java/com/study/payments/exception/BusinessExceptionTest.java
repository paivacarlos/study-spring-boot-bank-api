package com.study.payments.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@DisplayName("Unit Tests: BusinessException")
class BusinessExceptionTest {

    @Test
    @DisplayName("Should instantiate exception with message correctly")
    void shouldCreateExceptionWithMessage() {
        // ARRANGE & ACT
        String errorMessage =
                "Pix transaction cannot be cancelled because its current status is PAID";
        BusinessException exception = new BusinessException(errorMessage);

        // ASSERT
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause(), "Cause should be null when not provided");
    }

    @Test
    @DisplayName("Should instantiate exception with message and cause correctly")
    void shouldCreateExceptionWithMessageAndCause() {
        // ARRANGE
        String errorMessage = "Domain state transition error";
        Throwable rootCause = new IllegalStateException("Invalid status transition");

        // ACT
        BusinessException exception = new BusinessException(errorMessage, rootCause);

        // ASSERT
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(
                rootCause, exception.getCause(), "Root cause should be preserved in the exception");
    }

    @Test
    @DisplayName(
            "Should contain @ResponseStatus annotation mapped to HTTP 422 UNPROCESSABLE_ENTITY")
    void shouldHaveResponseStatusAnnotationWithUnprocessableEntity() {
        // ARRANGE & ACT: Inspeciona os metadados da classe via Reflexão do Java
        // (Reflection)
        ResponseStatus annotation = BusinessException.class.getAnnotation(ResponseStatus.class);

        // ASSERT: Garante o contrato com o Spring MVC para status HTTP 422
        assertNotNull(annotation, "Class should have @ResponseStatus annotation");
        assertEquals(
                HttpStatus.UNPROCESSABLE_ENTITY,
                annotation.value(),
                "HTTP status configured should be 422 UNPROCESSABLE_ENTITY");
    }
}
