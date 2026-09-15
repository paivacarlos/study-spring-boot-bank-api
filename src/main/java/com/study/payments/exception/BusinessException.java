package com.study.payments.exception;

// Exceção de negócio customizada que interrompe o fluxo de forma segura
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}