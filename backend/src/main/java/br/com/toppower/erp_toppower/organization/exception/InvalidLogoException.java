package br.com.toppower.erp_toppower.organization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando o upload de logo falha validações (content
 * type não permitido, arquivo vazio, etc.). Mapeada para HTTP 400
 * pelo {@code GlobalExceptionHandler}.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidLogoException extends RuntimeException {

    public InvalidLogoException(String message) {
        super(message);
    }
}