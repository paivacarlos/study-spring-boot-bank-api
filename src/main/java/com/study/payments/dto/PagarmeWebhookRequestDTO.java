package com.study.payments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record PagarmeWebhookRequestDTO(
        @NotBlank(message = "Charge ID is required")
        String id,

        @NotBlank(message = "Transaction code is required")
        String code,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        Integer amount,

        @NotBlank(message = "Status is required")
        String status,

        @JsonProperty("paid_at")
        @NotNull
        Instant paidAt
){
}