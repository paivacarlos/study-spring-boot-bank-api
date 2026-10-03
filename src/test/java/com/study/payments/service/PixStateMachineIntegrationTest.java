package com.study.payments.service;

import static org.junit.jupiter.api.Assertions.*;

import com.study.payments.dto.CreatePixRequestDTO;
import com.study.payments.dto.PagarmeWebhookRequestDTO;
import com.study.payments.dto.PixResponseDTO;
import com.study.payments.exception.BusinessException;
import com.study.payments.model.PixStatus;
import com.study.payments.repository.PixTransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("Integration Tests: Pix State Machine Life Cycle & Transitions")
class PixStateMachineIntegrationTest {

    @Autowired private PixService pixService;

    @Autowired private PixTransactionRepository repository;

    @Test
    @DisplayName("Should execute valid life cycle: CREATED -> PAID -> REFUNDED")
    void shouldExecuteValidLifeCycleFromCreatedToPaidToRefunded() {
        // 1. Criação da cobrança Pix (Estado inicial: CREATED)
        CreatePixRequestDTO createRequest =
                new CreatePixRequestDTO("1001-X", new BigDecimal("250.00"), "carlos@pix.com");
        PixResponseDTO createdPix = pixService.createPix(createRequest);
        UUID transactionId = createdPix.id();

        assertEquals(PixStatus.CREATED, pixService.findPixById(transactionId).status());

        // 2. Confirmação do pagamento via Webhook (Transição: CREATED -> PAID)
        PagarmeWebhookRequestDTO webhookRequest =
                new PagarmeWebhookRequestDTO(
                        "ch_gateway_123", transactionId.toString(), 25000, "paid", Instant.now());
        pixService.processWebhookConfirmation(webhookRequest);

        assertEquals(PixStatus.PAID, pixService.findPixById(transactionId).status());

        // 3. Estorno da cobrança paga (Transição: PAID -> REFUNDED)
        PixResponseDTO refundedPix = pixService.refundPix(transactionId);

        assertEquals(PixStatus.REFUNDED, refundedPix.status());
        assertEquals(PixStatus.REFUNDED, pixService.findPixById(transactionId).status());
    }

    @Test
    @DisplayName("Should execute valid life cycle: CREATED -> CANCELLED")
    void shouldExecuteValidLifeCycleFromCreatedToCancelled() {
        // 1. Criação da cobrança Pix (Estado inicial: CREATED)
        CreatePixRequestDTO createRequest =
                new CreatePixRequestDTO("2002-Y", new BigDecimal("100.00"), "joao@pix.com");
        PixResponseDTO createdPix = pixService.createPix(createRequest);
        UUID transactionId = createdPix.id();

        assertEquals(PixStatus.CREATED, pixService.findPixById(transactionId).status());

        // 2. Cancelamento da cobrança pendente (Transição: CREATED -> CANCELLED)
        PixResponseDTO cancelledPix = pixService.cancelPix(transactionId);

        assertEquals(PixStatus.CANCELLED, cancelledPix.status());
        assertEquals(PixStatus.CANCELLED, pixService.findPixById(transactionId).status());
    }

    @Test
    @DisplayName("Should reject invalid transition: Attempting to refund a CREATED transaction")
    void shouldRejectRefundOnCreatedTransaction() {
        // 1. Criação da cobrança Pix (Estado: CREATED)
        CreatePixRequestDTO createRequest =
                new CreatePixRequestDTO("3003-Z", new BigDecimal("80.00"), "maria@pix.com");
        PixResponseDTO createdPix = pixService.createPix(createRequest);
        UUID transactionId = createdPix.id();

        // 2. Tentativa inválida de estornar cobrança que não foi paga
        assertThrows(BusinessException.class, () -> pixService.refundPix(transactionId));

        // 3. Garante que o estado permaneceu CREATED
        assertEquals(PixStatus.CREATED, pixService.findPixById(transactionId).status());
    }

    @Test
    @DisplayName("Should reject invalid transition: Attempting to cancel a PAID transaction")
    void shouldRejectCancelOnPaidTransaction() {
        // 1. Criação da cobrança Pix e liquidação (Estado: PAID)
        CreatePixRequestDTO createRequest =
                new CreatePixRequestDTO("4004-W", new BigDecimal("300.00"), "ana@pix.com");
        PixResponseDTO createdPix = pixService.createPix(createRequest);
        UUID transactionId = createdPix.id();

        pixService.processWebhookConfirmation(
                new PagarmeWebhookRequestDTO(
                        "ch_gate_999", transactionId.toString(), 30000, "paid", Instant.now()));

        assertEquals(PixStatus.PAID, pixService.findPixById(transactionId).status());

        // 2. Tentativa inválida de cancelar cobrança já paga
        assertThrows(BusinessException.class, () -> pixService.cancelPix(transactionId));

        // 3. Garante que o estado permaneceu PAID
        assertEquals(PixStatus.PAID, pixService.findPixById(transactionId).status());
    }

    @Test
    @DisplayName(
            "Should reject operations on terminal state: Attempting to refund or cancel already"
                    + " REFUNDED transaction")
    void shouldRejectOperationsOnRefundedTransaction() {
        // 1. Criação, pagamento e estorno (Estado final: REFUNDED)
        CreatePixRequestDTO createRequest =
                new CreatePixRequestDTO("5005-K", new BigDecimal("50.00"), "pedro@pix.com");
        PixResponseDTO createdPix = pixService.createPix(createRequest);
        UUID transactionId = createdPix.id();

        pixService.processWebhookConfirmation(
                new PagarmeWebhookRequestDTO(
                        "ch_gate_888", transactionId.toString(), 5000, "paid", Instant.now()));
        pixService.refundPix(transactionId);

        assertEquals(PixStatus.REFUNDED, pixService.findPixById(transactionId).status());

        // 2. Re-estorno proibido
        assertThrows(BusinessException.class, () -> pixService.refundPix(transactionId));

        // 3. Cancelamento pós-estorno proibido
        assertThrows(BusinessException.class, () -> pixService.cancelPix(transactionId));

        // 4. Estado terminal permanece REFUNDED
        assertEquals(PixStatus.REFUNDED, pixService.findPixById(transactionId).status());
    }

    @Test
    @DisplayName(
            "Should reject operations on terminal state: Attempting to cancel or refund already"
                    + " CANCELLED transaction")
    void shouldRejectOperationsOnCancelledTransaction() {
        // 1. Criação e cancelamento (Estado: CANCELLED)
        CreatePixRequestDTO createRequest =
                new CreatePixRequestDTO("6006-J", new BigDecimal("120.00"), "lucas@pix.com");
        PixResponseDTO createdPix = pixService.createPix(createRequest);
        UUID transactionId = createdPix.id();

        pixService.cancelPix(transactionId);
        assertEquals(PixStatus.CANCELLED, pixService.findPixById(transactionId).status());

        // 2. Re-cancelamento proibido
        assertThrows(BusinessException.class, () -> pixService.cancelPix(transactionId));

        // 3. Estorno de cancelada proibido
        assertThrows(BusinessException.class, () -> pixService.refundPix(transactionId));

        // 4. Estado permanece CANCELLED
        assertEquals(PixStatus.CANCELLED, pixService.findPixById(transactionId).status());
    }
}
