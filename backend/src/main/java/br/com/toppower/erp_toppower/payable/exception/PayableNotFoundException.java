package br.com.toppower.erp_toppower.payable.exception;

public class PayableNotFoundException extends RuntimeException {

    public PayableNotFoundException(Long id) {
        super("Conta a pagar não encontrada: " + id);
    }
}