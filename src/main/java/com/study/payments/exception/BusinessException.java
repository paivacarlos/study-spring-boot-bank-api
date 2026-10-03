package com.study.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção de domínio customizada para violações de regras de negócio (ex.: transições proibidas na
 * máquina de estados de Pix).
 *
 * <p>A anotação @ResponseStatus instrui o Spring MVC a traduzir automaticamente esta exceção para
 * uma resposta HTTP 422 Unprocessable Entity sem necessidade de blocos try/catch manuais no
 * Controller.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
