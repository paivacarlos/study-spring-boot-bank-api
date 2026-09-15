package com.study.payments.service;

import com.study.payments.dto.CreatePixRequestDTO;
import com.study.payments.dto.PagarmeWebhookRequestDTO;
import com.study.payments.dto.PixResponseDTO;
import com.study.payments.exception.BusinessException;
import com.study.payments.model.PixStatus;
import com.study.payments.model.PixTransaction;
import com.study.payments.repository.PixTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void processWebhookConfirmation(PagarmeWebhookRequestDTO request) {
        log.info("Processing webhook payment confirmation for transaction code: {}, status: {}",
                request.code(), request.status());

        // 1. Conversão do code (String) para UUID
        UUID transactionId;
        try {
            transactionId = UUID.fromString(request.code());
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for transaction code: {}", request.code());
            throw new BusinessException("Invalid transaction code format: " + request.code());
        }

        // 2. Busca da transação no banco de dados H2
        PixTransaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> {
                    log.error("Transaction not found for ID: {}", transactionId);
                    return new BusinessException("Pix transaction not found: " + transactionId);
                });

        // 3. Trava de Idempotência: se já estiver PAID, ignora reprocessamento
        if (transaction.getStatus() == PixStatus.PAID) {
            log.warn("Transaction is already marked as PAID. Ignoring duplicate webhook. TransactionId: {}", transactionId);
            return;
        }

        // 4. Se já foi REFUNDED, não regride o status da cobrança
        if (transaction.getStatus() == PixStatus.REFUNDED) {
            log.warn("Late webhook received for already REFUNDED transaction. TransactionId: {}", transactionId);
            return;
        }

        // 5. Se estiver CANCELLED, alerta de pagamento tardio (será auto-estornado na Task 4.5)
        if (transaction.getStatus() == PixStatus.CANCELLED) {
            log.warn("Late payment received for CANCELLED transaction. TransactionId: {}. Flagged for auto-refund in Epic 04.", transactionId);
            return;
        }

        // 6. Transição legítima de estado: CREATED -> PAID
        transaction.setStatus(PixStatus.PAID);
        repository.save(transaction);

        log.info("Pix transaction successfully confirmed as PAID. TransactionId: {}", transactionId);
    }
}