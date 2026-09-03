package com.study.bank_api.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record PagarmeChargeResponseDTO(
        String id,
        String code,
        Integer amount,
        String status,

        @JsonProperty("paid_at")
        Instant paidAt
) {
}