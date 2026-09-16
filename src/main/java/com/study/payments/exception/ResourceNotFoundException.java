package com.study.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção de domínio disparada quando uma entidade solicitada não é localizada no banco de dados.
 * A anotação @ResponseStatus instrui o Spring MVC a mapear diretamente essa exceção
 * para uma resposta com status HTTP 404 Not Found, evitando necessidade de try/catch no Controller.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
