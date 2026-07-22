package br.com.toppower.erp_toppower.purchase.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando há erro na importação de NF-e (XML inválido,
 * parse falhou, nota já importada, etc.).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NfeImportException extends RuntimeException {

    public NfeImportException(String message) {
        super(message);
    }

    public NfeImportException(String message, Throwable cause) {
        super(message, cause);
    }
}