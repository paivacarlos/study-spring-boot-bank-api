package com.study.bank_api.service;

import com.study.bank_api.dto.CreatePixRequestDTO;
import com.study.bank_api.dto.PixResponseDTO;
import com.study.bank_api.model.PixTransaction;
import com.study.bank_api.repository.PixTransactionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PixService {

    private final PixTransactionRepository repository;

    // Injeção de dependência via construtor
    public PixService(PixTransactionRepository repository) {
        this.repository = repository;
    }

    public PixResponseDTO createPix(CreatePixRequestDTO request) {
        // 1. Geração de um identificador/payload simulado para o QR Code Copia e Cola
        String generatedQrCode = "00020126580014br.gov.bcb.pix0136" + request.pixKey() + "520400005303986540" + request.amount() + "5802BR5913BankApi6008BRASILIA62070503***6304" + UUID.randomUUID().toString().substring(0, 4);

        // 2. Criação da Entidade com os dados recebidos do DTO
        PixTransaction entity = new PixTransaction(
                request.accountNumber(),
                request.amount(),
                generatedQrCode,
                request.pixKey()
        );

        // 3. Persistência da entidade no banco de dados via JPA Repository
        PixTransaction savedEntity = repository.save(entity);

        // 4. Conversão da Entidade salva para o DTO de Saída
        return PixResponseDTO.fromEntity(savedEntity);
    }
}