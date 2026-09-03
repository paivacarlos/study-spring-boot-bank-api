package com.study.bank_api.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PagarmeChargeRequestDTO(
        @NotNull(message = "Amount in cents is required")
        @Positive(message = "Amount must be greater than zero")
        Integer amount,

        @JsonProperty("payment_method")
        String paymentMethod,

        String code
) {
}