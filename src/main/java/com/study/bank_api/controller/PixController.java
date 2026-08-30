package com.study.bank_api.controller;

import com.study.bank_api.dto.CreatePixRequestDTO;
import com.study.bank_api.dto.PixResponseDTO;
import com.study.bank_api.service.PixService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pix")
public class PixController {

    private final PixService pixService;

    // Injeção de dependência via construtor gerenciada pelo container IoC
    public PixController(PixService pixService) {
        this.pixService = pixService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PixResponseDTO createPix(@RequestBody @Valid CreatePixRequestDTO request) {
        return pixService.createPix(request);
    }
}