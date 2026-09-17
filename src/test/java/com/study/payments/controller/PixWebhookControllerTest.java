package com.study.payments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.payments.dto.PagarmeWebhookRequestDTO;
import com.study.payments.exception.ResourceNotFoundException;
import com.study.payments.service.PixService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PixWebhookController.class)
@DisplayName("Integration Tests: PixWebhookController (HTTP Layer)")
class PixWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PixService pixService;

    // =========================================================================
    // CENÁRIOS DE SUCESSO E IDEMPOTÊNCIA HTTP
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK when webhook payload is valid")
    void shouldReturn200OkWhenWebhookPayloadIsValid() throws Exception {
        // 1. ARRANGE: Payload válido de notificação da adquirente Pagar.me
        UUID transactionId = UUID.randomUUID();
        PagarmeWebhookRequestDTO validPayload = new PagarmeWebhookRequestDTO(
                "ch_1234567890",
                transactionId.toString(),
                15050,
                "paid",
                Instant.now()
        );

        doNothing().when(pixService).processWebhookConfirmation(any(PagarmeWebhookRequestDTO.class));

        // 2. ACT & ASSERT: Disparo HTTP POST e validação de retorno 200 OK
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload)))
                .andExpect(status().isOk());

        // Garante que o serviço de domínio foi acionado
        verify(pixService, times(1)).processWebhookConfirmation(any(PagarmeWebhookRequestDTO.class));
    }

    @Test
    @DisplayName("Should return 200 OK on repeated webhook calls to ensure gateway acknowledgment (Idempotency)")
    void shouldReturn200OkOnRepeatedWebhookCalls() throws Exception {
        // 1. ARRANGE: Mesma notificação reenviada pela adquirente por política de retentativa de rede
        UUID transactionId = UUID.randomUUID();
        PagarmeWebhookRequestDTO payload = new PagarmeWebhookRequestDTO(
                "ch_9876543210",
                transactionId.toString(),
                20000,
                "paid",
                Instant.now()
        );

        doNothing().when(pixService).processWebhookConfirmation(any(PagarmeWebhookRequestDTO.class));

        // 2. ACT: Primeira requisição
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // 3. ACT: Segunda requisição (duplicada)
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // ASSERT: O controller deve responder 200 OK em ambas sem falhar
        verify(pixService, times(2)).processWebhookConfirmation(any(PagarmeWebhookRequestDTO.class));
    }

    // =========================================================================
    // CENÁRIOS DE VALIDAÇÃO FAIL-FAST (Bean Validation / HTTP 400)
    // =========================================================================

    @Test
    @DisplayName("Should return 400 Bad Request when charge ID is blank")
    void shouldReturn400WhenChargeIdIsBlank() throws Exception {
        // ARRANGE: id vazio violando @NotBlank
        PagarmeWebhookRequestDTO invalidPayload = new PagarmeWebhookRequestDTO(
                "   ",
                UUID.randomUUID().toString(),
                15050,
                "paid",
                Instant.now()
        );

        // ACT & ASSERT: Barrado na porta de entrada
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());

        verify(pixService, never()).processWebhookConfirmation(any());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when transaction code is blank")
    void shouldReturn400WhenTransactionCodeIsBlank() throws Exception {
        // ARRANGE: code vazio violando @NotBlank
        PagarmeWebhookRequestDTO invalidPayload = new PagarmeWebhookRequestDTO(
                "ch_123456",
                "   ",
                15050,
                "paid",
                Instant.now()
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());

        verify(pixService, never()).processWebhookConfirmation(any());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when amount is zero or negative")
    void shouldReturn400WhenAmountIsZeroOrNegative() throws Exception {
        // ARRANGE: amount negativo violando @Positive
        PagarmeWebhookRequestDTO invalidPayload = new PagarmeWebhookRequestDTO(
                "ch_123456",
                UUID.randomUUID().toString(),
                -500,
                "paid",
                Instant.now()
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());

        verify(pixService, never()).processWebhookConfirmation(any());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when status is blank")
    void shouldReturn400WhenStatusIsBlank() throws Exception {
        // ARRANGE: status vazio violando @NotBlank
        PagarmeWebhookRequestDTO invalidPayload = new PagarmeWebhookRequestDTO(
                "ch_123456",
                UUID.randomUUID().toString(),
                15050,
                "",
                Instant.now()
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());

        verify(pixService, never()).processWebhookConfirmation(any());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when paid_at is null")
    void shouldReturn400WhenPaidAtIsNull() throws Exception {
        // ARRANGE: paid_at nulo violando @NotNull
        PagarmeWebhookRequestDTO invalidPayload = new PagarmeWebhookRequestDTO(
                "ch_123456",
                UUID.randomUUID().toString(),
                15050,
                "paid",
                null
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());

        verify(pixService, never()).processWebhookConfirmation(any());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request body is malformed JSON")
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        // ARRANGE: String com JSON sintaticamente quebrado
        String malformedJson = "{\"id\": \"ch_123\", \"code\": }";

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(pixService, never()).processWebhookConfirmation(any());
    }

    // =========================================================================
    // CENÁRIO DE DOMÍNIO / NÃO ENCONTRADO (HTTP 404)
    // =========================================================================

    @Test
    @DisplayName("Should return 404 Not Found when transaction does not exist in database")
    void shouldReturn404WhenTransactionDoesNotExist() throws Exception {
        // 1. ARRANGE: Service lança ResourceNotFoundException indicando que o ID não existe
        UUID nonExistentId = UUID.randomUUID();
        PagarmeWebhookRequestDTO payload = new PagarmeWebhookRequestDTO(
                "ch_123456",
                nonExistentId.toString(),
                15050,
                "paid",
                Instant.now()
        );

        doThrow(new ResourceNotFoundException("Transaction not found with ID: " + nonExistentId))
                .when(pixService).processWebhookConfirmation(any(PagarmeWebhookRequestDTO.class));

        // 2. ACT & ASSERT: Spring MVC traduz declarativamente a exceção para HTTP 404
        mockMvc.perform(post("/api/v1/pix/webhook/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());

        verify(pixService, times(1)).processWebhookConfirmation(any(PagarmeWebhookRequestDTO.class));
    }
}
