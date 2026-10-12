package com.study.payments.mock;

import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagarme-fake")
public class FakePagarmeController {

    private static final Logger log = LoggerFactory.getLogger(FakePagarmeController.class);

    @PostMapping("/charges")
    @ResponseStatus(HttpStatus.OK)
    public PagarmeChargeResponseDTO processCharge(
            @RequestBody @Valid PagarmeChargeRequestDTO request) {
        log.info(
                "Received fake Pagar.me charge request. Code: {}, Amount (cents): {},"
                        + " PaymentMethod: {}",
                request.code(),
                request.amount(),
                request.paymentMethod());

        // ID no padrão oficial documentado pela Pagar.me (prefixo "ch_")
        String fakeChargeId =
                "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // Status inicial de emissão Pix: aguardando liquidação pelo cliente
        String initialStatus = "waiting_payment";

        log.info(
                "Fake Pagar.me charge created with status '{}'. ChargeId: {}",
                initialStatus,
                fakeChargeId);

        return new PagarmeChargeResponseDTO(
                fakeChargeId,
                request.code(),
                request.amount(),
                initialStatus,
                null // TODO paid_at permanece nulo até a liquidação via Webhook no Épico 03
                );
    }

    @PostMapping("/charges/{id}/refund")
    @ResponseStatus(HttpStatus.OK)
    public PagarmeChargeResponseDTO refundCharge(
        @PathVariable String id,
        @RequestBody(required = false) PagarmeChargeRequestDTO request) {
        log.info("Received fake Pagar.me refund request for chargeId: {}", id);

        Integer amount = null;
        String code = null;

        if (request != null) {
            amount = request.amount();
            code = request.code();
        }

        log.info(
            "Fake Pagar.me charge refunded successfully. ChargeId: {}, Amount (cents): {}",
            id,
            amount);

        return new PagarmeChargeResponseDTO(
            id,
            code,
            amount,
            "refunded",
            null);
    }
}
