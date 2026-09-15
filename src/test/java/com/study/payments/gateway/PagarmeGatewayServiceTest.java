package com.study.payments.gateway;

import com.study.payments.exception.BusinessException;
import com.study.payments.mock.PagarmeChargeResponseDTO;
import com.study.payments.model.PixTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PagarmeGatewayServiceTest {

    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private PagarmeGatewayService gatewayService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080/pagarme-fake");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        gatewayService = new PagarmeGatewayService(restClient);
    }

    @Test
    @DisplayName("Should create charge successfully and convert amount to cents (Happy Path)")
    void shouldCreateChargeSuccessfully() {
        // 1. ARRANGE
        UUID transactionId = UUID.randomUUID();
        PixTransaction transaction = new PixTransaction(
                transactionId,
                "1001-X",
                new BigDecimal("150.50"),
                "mock-qrcode",
                "carlos@pix.com"
        );

        String simulatedSuccessResponse = """
                {
                    "id": "ch_mock123456",
                    "code": "%s",
                    "amount": 15050,
                    "status": "waiting_payment",
                    "paid_at": null
                }
                """.formatted(transactionId);

        mockServer.expect(requestTo("http://localhost:8080/pagarme-fake/charges"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.amount").value(15050))
                .andExpect(jsonPath("$.payment_method").value("pix"))
                .andExpect(jsonPath("$.code").value(transactionId.toString()))
                .andRespond(withSuccess(simulatedSuccessResponse, MediaType.APPLICATION_JSON));

        // 2. ACT
        PagarmeChargeResponseDTO response = gatewayService.createCharge(transaction);

        // 3. ASSERT
        assertNotNull(response);
        assertEquals("ch_mock123456", response.id());
        assertEquals("waiting_payment", response.status());
        assertEquals(15050, response.amount());
        assertEquals(transactionId.toString(), response.code());

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BusinessException when gateway returns HTTP 500 Internal Server Error")
    void shouldThrowBusinessExceptionWhenGatewayReturnsServerError() {
        // 1. ARRANGE
        PixTransaction transaction = new PixTransaction(
                UUID.randomUUID(),
                "1001-X",
                new BigDecimal("50.00"),
                "mock-qrcode",
                "carlos@pix.com"
        );

        mockServer.expect(requestTo("http://localhost:8080/pagarme-fake/charges"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // 2. ACT & ASSERT
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> gatewayService.createCharge(transaction)
        );

        assertTrue(exception.getMessage().contains("Payment gateway rejected the charge request"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BusinessException when gateway returns HTTP 400 Bad Request")
    void shouldThrowBusinessExceptionWhenGatewayReturnsBadRequest() {
        // 1. ARRANGE
        PixTransaction transaction = new PixTransaction(
                UUID.randomUUID(),
                "1001-X",
                new BigDecimal("25.00"),
                "mock-qrcode",
                "carlos@pix.com"
        );

        mockServer.expect(requestTo("http://localhost:8080/pagarme-fake/charges"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        // 2. ACT & ASSERT
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> gatewayService.createCharge(transaction)
        );

        assertTrue(exception.getMessage().contains("Payment gateway rejected the charge request"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BusinessException when gateway returns HTTP 422 Unprocessable Entity")
    void shouldThrowBusinessExceptionWhenGatewayReturnsUnprocessableEntity() {
        // 1. ARRANGE
        PixTransaction transaction = new PixTransaction(
                UUID.randomUUID(),
                "1001-X",
                new BigDecimal("75.00"),
                "mock-qrcode",
                "carlos@pix.com"
        );

        mockServer.expect(requestTo("http://localhost:8080/pagarme-fake/charges"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        // 2. ACT & ASSERT
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> gatewayService.createCharge(transaction)
        );

        assertTrue(exception.getMessage().contains("Payment gateway rejected the charge request"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BusinessException when network timeout or I/O failure occurs")
    void shouldThrowBusinessExceptionWhenNetworkTimeoutOccurs() {
        // 1. ARRANGE
        PixTransaction transaction = new PixTransaction(
                UUID.randomUUID(),
                "1001-X",
                new BigDecimal("100.00"),
                "mock-qrcode",
                "carlos@pix.com"
        );

        mockServer.expect(requestTo("http://localhost:8080/pagarme-fake/charges"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new IOException("Read timed out after 5000ms")));

        // 2. ACT & ASSERT
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> gatewayService.createCharge(transaction)
        );

        assertEquals("Payment gateway is currently unavailable or timed out. Please try again later.", exception.getMessage());
        mockServer.verify();
    }
}