package br.com.toppower.erp_toppower.client.exception;

public class DuplicateClientCodeException extends RuntimeException {

    public DuplicateClientCodeException(String code) {
        super("Já existe um cliente cadastrado com o código: " + code);
    }
}
