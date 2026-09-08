package com.study.payments.mock;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pagarme-fake")
public class FakePagarmeController {

    private static final Logger log = LoggerFactory.getLogger(FakePagarmeController.class);

    @PostMapping("/charges")
    @ResponseStatus(HttpStatus.OK)
    public PagarmeChargeResponseDTO processCharge(@RequestBody @Valid PagarmeChargeRequestDTO request) {
        log.info("Received fake Pagar.me charge request. Code: {}, Amount (cents): {}, PaymentMethod: {}",
                request.code(), request.amount(), request.paymentMethod());

        // ID no padrão oficial documentado pela Pagar.me (prefixo "ch_")
        String fakeChargeId = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // Status inicial de emissão Pix: aguardando liquidação pelo cliente
        String initialStatus = "waiting_payment";

        log.info("Fake Pagar.me charge created with status '{}'. ChargeId: {}", initialStatus, fakeChargeId);

        return new PagarmeChargeResponseDTO(
                fakeChargeId,
                request.code(),
                request.amount(),
                initialStatus,
                null // TODO paid_at permanece nulo até a liquidação via Webhook no Épico 03
        );
    }
}