package com.study.bank_api.dto;

import com.study.bank_api.model.PixStatus;
import com.study.bank_api.model.PixTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PixResponseDTO(
        UUID id,
        String accountNumber,
        BigDecimal amount,
        String qrCode,
        String pixKey,
        PixStatus status,
        LocalDateTime createdAt
) {
    // Método estático utilitário para converter uma Entity em DTO de saída
    public static PixResponseDTO fromEntity(PixTransaction transaction) {
        return new PixResponseDTO(
                transaction.getId(),
                transaction.getAccountNumber(),
                transaction.getAmount(),
                transaction.getQrCode(),
                transaction.getPixKey(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}