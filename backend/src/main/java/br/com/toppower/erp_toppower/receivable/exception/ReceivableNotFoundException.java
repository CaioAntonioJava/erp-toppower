package br.com.toppower.erp_toppower.receivable.exception;

public class ReceivableNotFoundException extends RuntimeException {

    public ReceivableNotFoundException(Long id) {
        super("Conta a receber não encontrada: " + id);
    }
}