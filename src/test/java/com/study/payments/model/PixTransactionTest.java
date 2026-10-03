package com.study.payments.model;

import static org.junit.jupiter.api.Assertions.*;

import com.study.payments.exception.BusinessException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Unit Tests: PixTransaction (Domain Entity & State Machine)")
class PixTransactionTest {

    private static final String ACCOUNT_NUMBER = "1001-X";
    private static final BigDecimal AMOUNT = new BigDecimal("150.50");
    private static final String QR_CODE = "00020126580014br.gov.bcb.pix...";
    private static final String PIX_KEY = "carlos@pix.com";

    // =========================================================================
    // TESTES: Construtores & Estado Inicial
    // =========================================================================

    @Test
    @DisplayName("Should initialize transaction with CREATED status using business constructor")
    void shouldInitializeWithCreatedStatusUsingBusinessConstructor() {
        // ACT: Criação via construtor padrão de negócio
        PixTransaction transaction = new PixTransaction(ACCOUNT_NUMBER, AMOUNT, QR_CODE, PIX_KEY);

        // ASSERT: Validação do estado inicial
        assertEquals(PixStatus.CREATED, transaction.getStatus());
        assertEquals(ACCOUNT_NUMBER, transaction.getAccountNumber());
        assertEquals(AMOUNT, transaction.getAmount());
        assertEquals(QR_CODE, transaction.getQrCode());
        assertEquals(PIX_KEY, transaction.getPixKey());
        assertNull(transaction.getId(), "ID should be null before persistence");
    }

    @Test
    @DisplayName(
            "Should initialize transaction with CREATED status using overloaded constructor with"
                    + " ID")
    void shouldInitializeWithCreatedStatusUsingOverloadedConstructorWithId() {
        // ARRANGE
        UUID id = UUID.randomUUID();

        // ACT: Criação com ID pré-definido (usado em testes e simulações)
        PixTransaction transaction =
                new PixTransaction(id, ACCOUNT_NUMBER, AMOUNT, QR_CODE, PIX_KEY);

        // ASSERT
        assertEquals(id, transaction.getId());
        assertEquals(PixStatus.CREATED, transaction.getStatus());
    }

    // =========================================================================
    // TESTES: Transição de Estorno (refund)
    // =========================================================================

    @Test
    @DisplayName("Should successfully transition from PAID to REFUNDED")
    void shouldRefundSuccessfullyWhenCurrentStatusIsPaid() {
        // ARRANGE: Cobrança que já foi paga
        PixTransaction transaction = new PixTransaction(ACCOUNT_NUMBER, AMOUNT, QR_CODE, PIX_KEY);
        transaction.setStatus(PixStatus.PAID);

        // ACT: Execução do estorno
        transaction.refund();

        // ASSERT: Estado deve ser estritamente REFUNDED
        assertEquals(PixStatus.REFUNDED, transaction.getStatus());
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = "PAID")
    @DisplayName("Should throw BusinessException when attempting refund from non-PAID status")
    void shouldThrowBusinessExceptionWhenRefundingFromInvalidStatus(PixStatus invalidStatus) {
        // ARRANGE: Cobrança em status proibido para estorno (CREATED, REFUNDED,
        // CANCELLED)
        PixTransaction transaction = new PixTransaction(ACCOUNT_NUMBER, AMOUNT, QR_CODE, PIX_KEY);
        transaction.setStatus(invalidStatus);

        // ACT & ASSERT: A entidade deve barrar a transição
        BusinessException exception = assertThrows(BusinessException.class, transaction::refund);

        assertTrue(exception.getMessage().contains("Pix transaction cannot be refunded"));
        assertTrue(exception.getMessage().contains(invalidStatus.name()));
        assertEquals(
                invalidStatus,
                transaction.getStatus(),
                "Status should remain unchanged after rejected operation");
    }

    // =========================================================================
    // TESTES: Transição de Cancelamento (cancel)
    // =========================================================================

    @Test
    @DisplayName("Should successfully transition from CREATED to CANCELLED")
    void shouldCancelSuccessfullyWhenCurrentStatusIsCreated() {
        // ARRANGE: Cobrança recém-gerada (status inicial CREATED)
        PixTransaction transaction = new PixTransaction(ACCOUNT_NUMBER, AMOUNT, QR_CODE, PIX_KEY);

        // ACT: Execução do cancelamento
        transaction.cancel();

        // ASSERT: Estado deve ser estritamente CANCELLED
        assertEquals(PixStatus.CANCELLED, transaction.getStatus());
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = "CREATED")
    @DisplayName("Should throw BusinessException when attempting cancel from non-CREATED status")
    void shouldThrowBusinessExceptionWhenCancellingFromInvalidStatus(PixStatus invalidStatus) {
        // ARRANGE: Cobrança em status proibido para cancelamento (PAID, REFUNDED,
        // CANCELLED)
        PixTransaction transaction = new PixTransaction(ACCOUNT_NUMBER, AMOUNT, QR_CODE, PIX_KEY);
        transaction.setStatus(invalidStatus);

        // ACT & ASSERT: A entidade deve barrar a transição
        BusinessException exception = assertThrows(BusinessException.class, transaction::cancel);

        assertTrue(exception.getMessage().contains("Pix transaction cannot be cancelled"));
        assertTrue(exception.getMessage().contains(invalidStatus.name()));
        assertEquals(
                invalidStatus,
                transaction.getStatus(),
                "Status should remain unchanged after rejected operation");
    }
}
