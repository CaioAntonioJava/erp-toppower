package br.com.toppower.erp_toppower.boleto.exception;

public class DuplicateBoletoDescriptionException extends RuntimeException {

    public DuplicateBoletoDescriptionException(String description) {
        super("Já existe um boleto cadastrado com a descrição: " + description);
    }
}
