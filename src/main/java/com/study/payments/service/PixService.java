package com.study.payments.service;
import com.study.payments.dto.PagarmeWebhookRequestDTO;

import com.study.payments.dto.CreatePixRequestDTO;
import com.study.payments.dto.PixResponseDTO;
import com.study.payments.model.PixTransaction;
import com.study.payments.repository.PixTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PixService {

    private static final Logger log = LoggerFactory.getLogger(PixService.class);

    private final PixTransactionRepository repository;

    public PixService(PixTransactionRepository repository) {
        this.repository = repository;
    }

    public PixResponseDTO createPix(CreatePixRequestDTO request) {
        log.info("Initiating creation of Pix payment request for account: {}, amount: {}",
                request.accountNumber(), request.amount());

        // 1. Geração de payload simulado para o QR Code
        String generatedQrCode = "00020126580014br.gov.bcb.pix0136" + request.pixKey() +
                "520400005303986540" + request.amount() +
                "5802BR5913BankApi6008BRASILIA62070503***6304" +
                UUID.randomUUID().toString().substring(0, 4);

        // 2. Criação da Entidade
        PixTransaction entity = new PixTransaction(
                request.accountNumber(),
                request.amount(),
                generatedQrCode,
                request.pixKey()
        );

        // 3. Persistência da entidade no H2
        PixTransaction savedEntity = repository.save(entity);
        log.info("Pix payment request successfully created. TransactionId: {}, Status: {}",
                savedEntity.getId(), savedEntity.getStatus());

        // 4. Retorno do DTO
        return PixResponseDTO.fromEntity(savedEntity);
    }

        public void processWebhookConfirmation(PagarmeWebhookRequestDTO request) {
        log.info("Processing webhook payment confirmation for transaction code: {}, status: {}",
                request.code(), request.status());

        // TODO: Na TASK-3.2 implementaremos a busca por ID, idempotência e atualização para PAID com @Transactional
    }

}