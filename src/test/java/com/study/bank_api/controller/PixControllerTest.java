package com.study.bank_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.bank_api.dto.CreatePixRequestDTO;
import com.study.bank_api.dto.PixResponseDTO;
import com.study.bank_api.model.PixStatus;
import com.study.bank_api.service.PixService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PixController.class)
class PixControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock do serviço para focar apenas nas validações de rota HTTP e Bean Validation
    @MockitoBean
    private PixService pixService;

    @Test
    @DisplayName("Should return 201 Created and response body when payload is valid")
    void shouldReturn201WhenPayloadIsValid() throws Exception {
        // 1. ARRANGE: Criação do DTO de entrada e do DTO esperado de saída
        CreatePixRequestDTO request = new CreatePixRequestDTO(
                "1001-X",
                new BigDecimal("150.50"),
                "carlos@pix.com"
        );

        PixResponseDTO expectedResponse = new PixResponseDTO(
                UUID.randomUUID(),
                "1001-X",
                new BigDecimal("150.50"),
                "00020126580014br.gov.bcb.pix0136...",
                "carlos@pix.com",
                PixStatus.CREATED,
                LocalDateTime.now()
        );

        when(pixService.createPix(any(CreatePixRequestDTO.class))).thenReturn(expectedResponse);

        // 2. ACT & ASSERT: Disparo da requisição HTTP POST e validações de cabeçalho e JSON
        mockMvc.perform(post("/api/v1/pix")
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
        CreatePixRequestDTO invalidRequest = new CreatePixRequestDTO(
                "1001-X",
                new BigDecimal("-50.00"),
                "carlos@pix.com"
        );

        // 2. ACT & ASSERT: O Bean Validation deve barrar na porta de entrada
        mockMvc.perform(post("/api/v1/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when accountNumber is blank")
    void shouldReturn400WhenAccountNumberIsBlank() throws Exception {
        // ARRANGE: Payload com accountNumber em branco ("   ") violando @NotBlank
        CreatePixRequestDTO invalidRequest = new CreatePixRequestDTO(
                "   ",
                new BigDecimal("150.50"),
                "carlos@pix.com"
        );

        // ACT & ASSERT: Bloqueio na porta de entrada pelo Bean Validation
        mockMvc.perform(post("/api/v1/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when amount is null")
    void shouldReturn400WhenAmountIsNull() throws Exception {
        // ARRANGE: Payload sem o campo amount (null) violando @NotNull
        CreatePixRequestDTO invalidRequest = new CreatePixRequestDTO(
                "1001-X",
                null,
                "carlos@pix.com"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when amount is zero")
    void shouldReturn400WhenAmountIsZero() throws Exception {
        // ARRANGE: Payload com valor zero violando @Positive (exige estritamente > 0)
        CreatePixRequestDTO invalidRequest = new CreatePixRequestDTO(
                "1001-X",
                BigDecimal.ZERO,
                "carlos@pix.com"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when pixKey is empty")
    void shouldReturn400WhenPixKeyIsEmpty() throws Exception {
        // ARRANGE: Payload com chave Pix vazia ("") violando @NotBlank
        CreatePixRequestDTO invalidRequest = new CreatePixRequestDTO(
                "1001-X",
                new BigDecimal("150.50"),
                ""
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/pix")
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
        mockMvc.perform(post("/api/v1/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }
}