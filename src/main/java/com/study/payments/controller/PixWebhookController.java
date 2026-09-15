package com.study.payments.controller;

import com.study.payments.dto.PagarmeWebhookRequestDTO;
import com.study.payments.service.PixService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pix/webhook")
public class PixWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PixWebhookController.class);

    private final PixService pixService;

    public PixWebhookController(PixService pixService) {
        this.pixService = pixService;
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.OK)
    public void confirmPayment(@RequestBody @Valid PagarmeWebhookRequestDTO request) {
        log.info("Received payment webhook confirmation for transaction code: {}, status: {}",
                request.code(), request.status());

        pixService.processWebhookConfirmation(request);
    }
}
