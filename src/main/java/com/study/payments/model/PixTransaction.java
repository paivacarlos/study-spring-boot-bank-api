package com.study.payments.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pix_transactions")
public class PixTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 1000)
    private String qrCode;

    @Column(nullable = false)
    private String pixKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PixStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Construtor protegido exigido pela especificação JPA/Hibernate
    protected PixTransaction() {
    }

    // Construtor de negócio para instanciar uma nova cobrança com status inicial CREATED
    public PixTransaction(String accountNumber, BigDecimal amount, String qrCode, String pixKey) {
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.qrCode = qrCode;
        this.pixKey = pixKey;
        this.status = PixStatus.CREATED;
    }

    // Adicione este construtor público na PixTransaction.java:
    public PixTransaction(UUID id, String accountNumber, BigDecimal amount, String qrCode, String pixKey) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.qrCode = qrCode;
        this.pixKey = pixKey;
        this.status = PixStatus.CREATED;
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getPixKey() {
        return pixKey;
    }

    public PixStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setStatus(PixStatus status) {
        this.status = status;
    }
}