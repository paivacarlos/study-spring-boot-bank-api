package com.study.bank_api.controller;

import com.study.bank_api.dto.CreatePixRequestDTO;
import com.study.bank_api.dto.PixResponseDTO;
import com.study.bank_api.service.PixService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pix")
public class PixController {

    private static final Logger log = LoggerFactory.getLogger(PixController.class);

    private final PixService pixService;

    public PixController(PixService pixService) {
        this.pixService = pixService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PixResponseDTO createPix(@RequestBody @Valid CreatePixRequestDTO request) {
        log.info("Received POST /api/v1/pix request for account: {}", request.accountNumber());
        return pixService.createPix(request);
    }
}