package com.study.payments.service;

import com.study.payments.dto.CreatePixRequestDTO;
import com.study.payments.dto.PagarmeWebhookRequestDTO;
import com.study.payments.dto.PixResponseDTO;
import com.study.payments.exception.BusinessException;
import com.study.payments.model.PixStatus;
import com.study.payments.model.PixTransaction;
import com.study.payments.repository.PixTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixServiceTest {

    @Mock
    private PixTransactionRepository repository;

    @InjectMocks
    private PixService pixService;

    // =========================================================================
    // TESTES: createPix
    // =========================================================================

    @Test
    @DisplayName("Should create Pix transaction successfully when valid payload is provided")
    void shouldCreatePixTransactionSuccessfully() {
        // 1. ARRANGE
        CreatePixRequestDTO request = new CreatePixRequestDTO(
                "1001-X",
                new BigDecimal("150.50"),
                "carlos@pix.com"
        );

        PixTransaction simulatedEntity = new PixTransaction(
                request.accountNumber(),
                request.amount(),
                "00020126580014br.gov.bcb.pix0136carlos@pix.com...",
                request.pixKey()
        );

        when(repository.save(any(PixTransaction.class))).thenReturn(simulatedEntity);

        // 2. ACT
        PixResponseDTO response = pixService.createPix(request);

        // 3. ASSERT
        assertNotNull(response);
        assertEquals(request.accountNumber(), response.accountNumber());
        assertEquals(request.amount(), response.amount());
        assertEquals(request.pixKey(), response.pixKey());
        assertEquals(PixStatus.CREATED, response.status());
        assertNotNull(response.qrCode());

        verify(repository, times(1)).save(any(PixTransaction.class));
    }

    @Test
    @DisplayName("Should throw exception when database repository fails to persist transaction")
    void shouldThrowExceptionWhenRepositoryFailsToSave() {
        // 1. ARRANGE
        CreatePixRequestDTO request = new CreatePixRequestDTO(
                "1001-X",
                new BigDecimal("150.50"),
                "carlos@pix.com"
        );

        when(repository.save(any(PixTransaction.class)))
                .thenThrow(new RuntimeException("Database connection timeout"));

        // 2. ACT & ASSERT
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pixService.createPix(request)
        );

        assertEquals("Database connection timeout", exception.getMessage());
        verify(repository, times(1)).save(any(PixTransaction.class));
    }

    @Test
    @DisplayName("Should throw NullPointerException when create request payload is null (Defense in Depth)")
    void shouldThrowExceptionWhenCreateRequestIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> pixService.createPix(null)
        );

        verify(repository, never()).save(any(PixTransaction.class));
    }

    // =========================================================================
    // TESTES: processWebhookConfirmation (Cenários Positivos & Idempotência)
    // =========================================================================

    @Test
    @DisplayName("Should confirm payment successfully when transaction is CREATED")
    void shouldConfirmPaymentSuccessfullyWhenTransactionIsCreated() {
        // 1. ARRANGE
        UUID transactionId = UUID.randomUUID();
        PixTransaction entity = new PixTransaction(
                transactionId,
                "1001-X",
                new BigDecimal("150.50"),
                "0002012658...",
                "carlos@pix.com"
        );
        assertEquals(PixStatus.CREATED, entity.getStatus());

        PagarmeWebhookRequestDTO request = new PagarmeWebhookRequestDTO(
                "ch_123456",
                transactionId.toString(),
                15050,
                "paid",
                Instant.now()
        );

        when(repository.findById(transactionId)).thenReturn(Optional.of(entity));
        when(repository.save(any(PixTransaction.class))).thenReturn(entity);

        // 2. ACT
        pixService.processWebhookConfirmation(request);

        // 3. ASSERT
        assertEquals(PixStatus.PAID, entity.getStatus());
        verify(repository, times(1)).findById(transactionId);
        verify(repository, times(1)).save(entity);
    }

    @Test
    @DisplayName("Should be idempotent and ignore duplicate webhook when transaction is already PAID")
    void shouldBeIdempotentAndIgnoreWhenTransactionIsAlreadyPaid() {
        // 1. ARRANGE: Entidade já liquidada anteriormente
        UUID transactionId = UUID.randomUUID();
        PixTransaction entity = new PixTransaction(
                transactionId,
                "1001-X",
                new BigDecimal("150.50"),
                "0002012658...",
                "carlos@pix.com"
        );
        entity.setStatus(PixStatus.PAID);

        PagarmeWebhookRequestDTO duplicateRequest = new PagarmeWebhookRequestDTO(
                "ch_123456",
                transactionId.toString(),
                15050,
                "paid",
                Instant.now()
        );

        when(repository.findById(transactionId)).thenReturn(Optional.of(entity));

        // 2. ACT
        pixService.processWebhookConfirmation(duplicateRequest);

        // 3. ASSERT: Idempotência garantida - status inalterado e NENHUMA escrita no banco
        assertEquals(PixStatus.PAID, entity.getStatus());
        verify(repository, times(1)).findById(transactionId);
        verify(repository, never()).save(any(PixTransaction.class));
    }

    @Test
    @DisplayName("Should ignore late webhook when transaction is already REFUNDED without regressing state")
    void shouldIgnoreLateWebhookWhenTransactionIsRefunded() {
        // 1. ARRANGE: Entidade já estornada
        UUID transactionId = UUID.randomUUID();
        PixTransaction entity = new PixTransaction(
                transactionId,
                "1001-X",
                new BigDecimal("150.50"),
                "0002012658...",
                "carlos@pix.com"
        );
        entity.setStatus(PixStatus.REFUNDED);

        PagarmeWebhookRequestDTO lateRequest = new PagarmeWebhookRequestDTO(
                "ch_123456",
                transactionId.toString(),
                15050,
                "paid",
                Instant.now()
        );

        when(repository.findById(transactionId)).thenReturn(Optional.of(entity));

        // 2. ACT
        pixService.processWebhookConfirmation(lateRequest);

        // 3. ASSERT: Não regride para PAID e não chama save
        assertEquals(PixStatus.REFUNDED, entity.getStatus());
        verify(repository, times(1)).findById(transactionId);
        verify(repository, never()).save(any(PixTransaction.class));
    }

    @Test
    @DisplayName("Should log warning and ignore when transaction is CANCELLED (Late Payment)")
    void shouldIgnoreWhenTransactionIsCancelled() {
        // 1. ARRANGE: Entidade expirada/cancelada
        UUID transactionId = UUID.randomUUID();
        PixTransaction entity = new PixTransaction(
                transactionId,
                "1001-X",
                new BigDecimal("150.50"),
                "0002012658...",
                "carlos@pix.com"
        );
        entity.setStatus(PixStatus.CANCELLED);

        PagarmeWebhookRequestDTO request = new PagarmeWebhookRequestDTO(
                "ch_123456",
                transactionId.toString(),
                15050,
                "paid",
                Instant.now()
        );

        when(repository.findById(transactionId)).thenReturn(Optional.of(entity));

        // 2. ACT
        pixService.processWebhookConfirmation(request);

        // 3. ASSERT: Não altera status cegamente e não persiste antes da rotina da Task 4.5
        assertEquals(PixStatus.CANCELLED, entity.getStatus());
        verify(repository, times(1)).findById(transactionId);
        verify(repository, never()).save(any(PixTransaction.class));
    }

    // =========================================================================
    // TESTES: processWebhookConfirmation (Stress de Cenários Negativos & Falhas)
    // =========================================================================

    @Test
    @DisplayName("Should throw BusinessException when transaction code does not exist in repository")
    void shouldThrowBusinessExceptionWhenTransactionNotFound() {
        // 1. ARRANGE
        UUID nonExistentId = UUID.randomUUID();
        PagarmeWebhookRequestDTO request = new PagarmeWebhookRequestDTO(
                "ch_123456",
                nonExistentId.toString(),
                15050,
                "paid",
                Instant.now()
        );

        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // 2. ACT & ASSERT
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pixService.processWebhookConfirmation(request)
        );

        assertTrue(exception.getMessage().contains("Pix transaction not found"));
        verify(repository, times(1)).findById(nonExistentId);
        verify(repository, never()).save(any(PixTransaction.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when transaction code has invalid UUID format")
    void shouldThrowBusinessExceptionWhenCodeIsInvalidUUID() {
        // 1. ARRANGE: String corrompida que não segue o padrão UUID RFC 4122
        PagarmeWebhookRequestDTO malformedRequest = new PagarmeWebhookRequestDTO(
                "ch_123456",
                "codigo-invalido-nao-uuid",
                15050,
                "paid",
                Instant.now()
        );

        // 2. ACT & ASSERT
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pixService.processWebhookConfirmation(malformedRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid transaction code format"));
        verify(repository, never()).findById(any(UUID.class));
        verify(repository, never()).save(any(PixTransaction.class));
    }

    @Test
    @DisplayName("Should throw NullPointerException when webhook request payload is null (Defense in Depth)")
    void shouldThrowExceptionWhenWebhookRequestIsNull() {
        // ACT & ASSERT
        assertThrows(
                NullPointerException.class,
                () -> pixService.processWebhookConfirmation(null)
        );

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should propagate exception when repository findById fails due to database outage")
    void shouldPropagateExceptionWhenDatabaseFailsOnFindById() {
        // 1. ARRANGE: Simulação de queda de conexão com o banco relacional
        UUID transactionId = UUID.randomUUID();
        PagarmeWebhookRequestDTO request = new PagarmeWebhookRequestDTO(
                "ch_123456",
                transactionId.toString(),
                15050,
                "paid",
                Instant.now()
        );

        when(repository.findById(transactionId))
                .thenThrow(new RuntimeException("Database connection timeout during find"));

        // 2. ACT & ASSERT
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pixService.processWebhookConfirmation(request)
        );

        assertEquals("Database connection timeout during find", exception.getMessage());
        verify(repository, times(1)).findById(transactionId);
        verify(repository, never()).save(any(PixTransaction.class));
    }

    @Test
    @DisplayName("Should propagate exception when repository save fails to ensure transactional rollback")
    void shouldPropagateExceptionWhenDatabaseFailsOnSave() {
        // 1. ARRANGE: Falha durante o commit/save no banco
        UUID transactionId = UUID.randomUUID();
        PixTransaction entity = new PixTransaction(
                transactionId,
                "1001-X",
                new BigDecimal("150.50"),
                "0002012658...",
                "carlos@pix.com"
        );

        PagarmeWebhookRequestDTO request = new PagarmeWebhookRequestDTO(
                "ch_123456",
                transactionId.toString(),
                15050,
                "paid",
                Instant.now()
        );

        when(repository.findById(transactionId)).thenReturn(Optional.of(entity));
        when(repository.save(any(PixTransaction.class)))
                .thenThrow(new RuntimeException("Database disk full on save"));

        // 2. ACT & ASSERT: A exceção deve subir para disparar o rollback do @Transactional
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pixService.processWebhookConfirmation(request)
        );

        assertEquals("Database disk full on save", exception.getMessage());
        verify(repository, times(1)).findById(transactionId);
        verify(repository, times(1)).save(entity);
    }
}