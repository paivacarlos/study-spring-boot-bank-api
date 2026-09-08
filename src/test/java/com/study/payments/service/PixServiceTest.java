package com.study.payments.service;

import com.study.payments.dto.CreatePixRequestDTO;
import com.study.payments.dto.PixResponseDTO;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixServiceTest {

    @Mock
    private PixTransactionRepository repository;

    @InjectMocks
    private PixService pixService;

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
    @DisplayName("Should throw NullPointerException when request payload is null (Defense in Depth)")
    void shouldThrowExceptionWhenRequestIsNull() {
        // 1. ARRANGE
        CreatePixRequestDTO nullRequest = null;

        // 2. ACT & ASSERT
        assertThrows(
                NullPointerException.class,
                () -> pixService.createPix(nullRequest)
        );

        verify(repository, never()).save(any(PixTransaction.class));
    }
}