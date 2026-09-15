package com.study.payments.gateway;

import com.study.payments.exception.BusinessException;
import com.study.payments.mock.PagarmeChargeRequestDTO;
import com.study.payments.mock.PagarmeChargeResponseDTO;
import com.study.payments.model.PixTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Service
public class PagarmeGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PagarmeGatewayService.class);

    private final RestClient pagarmeRestClient;

    // Injeção de dependência do RestClient configurado no RestClientConfig
    public PagarmeGatewayService(RestClient pagarmeRestClient) {
        this.pagarmeRestClient = pagarmeRestClient;
    }

    /**
     * Envia a transação Pix para processamento na adquirente externa (Pagar.me).
     * Aplica a Anti-Corruption Layer (ACL): converte BigDecimal em centavos inteiros.
     */
    public PagarmeChargeResponseDTO createCharge(PixTransaction transaction) {
        // 1. Conversão do domínio interno (BigDecimal) para o padrão do gateway (centavos)
        Integer amountInCents = transaction.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        // 2. Montagem do payload de integração exigido pelo gateway
        PagarmeChargeRequestDTO requestPayload = new PagarmeChargeRequestDTO(
                amountInCents,
                "pix",
                transaction.getId().toString()
        );

        log.info("Sending charge request to Pagar.me Gateway. Code: {}, Amount (cents): {}",
                requestPayload.code(), requestPayload.amount());

        // 3. Disparo da requisição HTTP POST via RestClient com resiliência e tratamento de falhas
        try {
            PagarmeChargeResponseDTO response = pagarmeRestClient.post()
                    .uri("/charges")
                    .body(requestPayload)
                    .retrieve()
                    .body(PagarmeChargeResponseDTO.class);

            log.info("Received charge response from Pagar.me Gateway. ChargeId: {}, Status: {}",
                    response.id(), response.status());

            return response;

        } catch (ResourceAccessException ex) {
            // Captura timeouts (ConnectTimeout / ReadTimeout) ou gateway fora do ar (erro de rede I/O)
            log.error("Network timeout or gateway unavailable when communicating with Pagar.me. TransactionId: {}",
                    transaction.getId(), ex);
            throw new BusinessException("Payment gateway is currently unavailable or timed out. Please try again later.");

        } catch (RestClientResponseException ex) {
            // Captura respostas HTTP de erro (status 4xx ou 5xx) devolvidas pelo gateway externo
            log.error("Pagar.me Gateway returned an HTTP error. Status: {}, ResponseBody: {}, TransactionId: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), transaction.getId(), ex);
            throw new BusinessException("Payment gateway rejected the charge request: " + ex.getStatusCode());

        } catch (Exception ex) {
            // Captura qualquer outra falha imprevista durante a integração
            log.error("Unexpected error during gateway charge integration. TransactionId: {}",
                    transaction.getId(), ex);
            throw new BusinessException("Internal integration failure while processing payment.");
        }
    }
}