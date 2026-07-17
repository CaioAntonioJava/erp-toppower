package br.com.toppower.erp_toppower.boleto.exception;

public class BoletoNotFoundException extends RuntimeException {

    public BoletoNotFoundException(Long id) {
        super("Boleto não encontrado: " + id);
    }
}