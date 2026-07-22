package br.com.toppower.erp_toppower.boleto.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando uma operação de liquidação é tentada em um
 * boleto que já foi liquidado anteriormente.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BoletoAlreadyPaidException extends RuntimeException {

    public BoletoAlreadyPaidException(Long id) {
        super("Boleto " + id + " já foi liquidado.");
    }
}
