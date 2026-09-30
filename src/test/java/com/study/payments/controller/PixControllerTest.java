package com.study.payments.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.payments.dto.CreatePixRequestDTO;
import com.study.payments.dto.PixResponseDTO;
import com.study.payments.exception.ResourceNotFoundException;
import com.study.payments.model.PixStatus;
import com.study.payments.service.PixService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PixController.class)
class PixControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    // Mock do serviço para focar apenas nas validações de rota HTTP e Bean
    // Validation
    @MockitoBean private PixService pixService;

    @Test
    @DisplayName("Should return 201 Created and response body when payload is valid")
    void shouldReturn201WhenPayloadIsValid() throws Exception {
        // 1. ARRANGE: Criação do DTO de entrada e do DTO esperado de saída
        CreatePixRequestDTO request =
                new CreatePixRequestDTO("1001-X", new BigDecimal("150.50"), "carlos@pix.com");

        PixResponseDTO expectedResponse =
                new PixResponseDTO(
                        UUID.randomUUID(),
                        "1001-X",
                        new BigDecimal("150.50"),
                        "00020126580014br.gov.bcb.pix0136...",
                        "carlos@pix.com",
                        PixStatus.CREATED,
                        LocalDateTime.now());

        when(pixService.createPix(any(CreatePixRequestDTO.class))).thenReturn(expectedResponse);

        // 2. ACT & ASSERT: Disparo da requisição HTTP POST e validações de cabeçalho e
        // JSON
        mockMvc.perform(
                        post("/api/v1/pix")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("1001-X"))
                .andExpect(jsonPath("$.amount").value(150.50))
                .andExpect(jsonPath("$.pixKey").value("carlos@pix.com"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.qrCode").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when amount is negative (Fail-Fast Validation)")
    void shouldReturn400WhenAmountIsNegative() throws Exception {
        // 1. ARRANGE: Payload com valor monetário negativo violando @Positive
        CreatePixRequestDTO invalidRequest =
                new CreatePixRequestDTO("1001-X", new BigDecimal("-50.00"), "carlos@pix.com");

        // 2. ACT & ASSERT: O Bean Validation deve barrar na porta de entrada
        mockMvc.perform(
                        post("/api/v1/pix")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when accountNumber is blank")
    void shouldReturn400WhenAccountNumberIsBlank() throws Exception {
        // ARRANGE: Payload com accountNumber em branco (" ") violando @NotBlank
        CreatePixRequestDTO invalidRequest =
                new CreatePixRequestDTO("   ", new BigDecimal("150.50"), "carlos@pix.com");

        // ACT & ASSERT: Bloqueio na porta de entrada pelo Bean Validation
        mockMvc.perform(
                        post("/api/v1/pix")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when amount is null")
    void shouldReturn400WhenAmountIsNull() throws Exception {
        // ARRANGE: Payload sem o campo amount (null) violando @NotNull
        CreatePixRequestDTO invalidRequest =
                new CreatePixRequestDTO("1001-X", null, "carlos@pix.com");

        // ACT & ASSERT
        mockMvc.perform(
                        post("/api/v1/pix")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when amount is zero")
    void shouldReturn400WhenAmountIsZero() throws Exception {
        // ARRANGE: Payload com valor zero violando @Positive (exige estritamente > 0)
        CreatePixRequestDTO invalidRequest =
                new CreatePixRequestDTO("1001-X", BigDecimal.ZERO, "carlos@pix.com");

        // ACT & ASSERT
        mockMvc.perform(
                        post("/api/v1/pix")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when pixKey is empty")
    void shouldReturn400WhenPixKeyIsEmpty() throws Exception {
        // ARRANGE: Payload com chave Pix vazia ("") violando @NotBlank
        CreatePixRequestDTO invalidRequest =
                new CreatePixRequestDTO("1001-X", new BigDecimal("150.50"), "");

        // ACT & ASSERT
        mockMvc.perform(
                        post("/api/v1/pix")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when JSON body is malformed or invalid")
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        // ARRANGE: String de JSON quebrada/inválida que causa erro no parser do Jackson
        String malformedJson = "{ \"accountNumber\": \"1001-X\", \"amount\": }";

        // ACT & ASSERT: O Spring rejeita antes mesmo da validação dos atributos
        mockMvc.perform(
                        post("/api/v1/pix")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // TESTES: getPixById (GET /api/v1/pix/{id})
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK and Pix details when transaction exists")
    void shouldReturn200AndPixDetailsWhenIdExists() throws Exception {
        // 1. ARRANGE
        UUID transactionId = UUID.randomUUID();
        PixResponseDTO expectedResponse =
                new PixResponseDTO(
                        transactionId,
                        "1001-X",
                        new BigDecimal("150.50"),
                        "00020126580014br.gov.bcb.pix...",
                        "carlos@pix.com",
                        PixStatus.PAID,
                        LocalDateTime.now());

        when(pixService.findPixById(transactionId)).thenReturn(expectedResponse);

        // 2. ACT & ASSERT
        mockMvc.perform(get("/api/v1/pix/{id}", transactionId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("1001-X"))
                .andExpect(jsonPath("$.amount").value(150.50))
                .andExpect(jsonPath("$.pixKey").value("carlos@pix.com"))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when transaction does not exist")
    void shouldReturn404NotFoundWhenTransactionDoesNotExist() throws Exception {
        // 1. ARRANGE: Serviço lança ResourceNotFoundException
        UUID nonExistentId = UUID.randomUUID();
        when(pixService.findPixById(nonExistentId))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Pix transaction not found with ID: " + nonExistentId));

        // 2. ACT & ASSERT: Spring MVC traduz para HTTP 404
        mockMvc.perform(get("/api/v1/pix/{id}", nonExistentId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when path variable is not a valid UUID")
    void shouldReturn400BadRequestWhenIdIsNotAValidUUID() throws Exception {
        // 1. ACT & ASSERT: Passando uma string arbitrária que não obedece o padrão UUID
        mockMvc.perform(
                        get("/api/v1/pix/{id}", "chave-invalida-123")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // TESTES: refundPix (POST /api/v1/pix/{id}/refund)
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK and refunded Pix details when refund request is successful")
    void shouldReturn200AndRefundedPixDetailsWhenRefundIsSuccessful() throws Exception {
        // 1. ARRANGE: Prepara o DTO simulado com status REFUNDED
        UUID transactionId = UUID.randomUUID();
        PixResponseDTO expectedResponse =
                new PixResponseDTO(
                        transactionId,
                        "1001-X",
                        new BigDecimal("150.50"),
                        "00020126580014br.gov.bcb.pix...",
                        "carlos@pix.com",
                        PixStatus.REFUNDED,
                        LocalDateTime.now());

        when(pixService.refundPix(transactionId)).thenReturn(expectedResponse);

        // 2. ACT & ASSERT: Dispara POST na rota de refund e valida a resposta HTTP 200
        mockMvc.perform(
                        post("/api/v1/pix/{id}/refund", transactionId)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("1001-X"))
                .andExpect(jsonPath("$.amount").value(150.50))
                .andExpect(jsonPath("$.pixKey").value("carlos@pix.com"))
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when attempting to refund non-existent transaction")
    void shouldReturn404NotFoundWhenRefundingNonExistentTransaction() throws Exception {
        // 1. ARRANGE: O serviço lança ResourceNotFoundException
        UUID nonExistentId = UUID.randomUUID();
        when(pixService.refundPix(nonExistentId))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Pix transaction not found with ID: " + nonExistentId));

        // 2. ACT & ASSERT: Spring MVC traduz automaticamente a exceção para HTTP 404
        mockMvc.perform(
                        post("/api/v1/pix/{id}/refund", nonExistentId)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when path variable for refund is not a valid UUID")
    void shouldReturn400BadRequestWhenRefundIdIsNotAValidUUID() throws Exception {
        // 1. ACT & ASSERT: Passa uma string malformada na URL
        mockMvc.perform(
                        post("/api/v1/pix/{id}/refund", "uuid-invalido-123")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
