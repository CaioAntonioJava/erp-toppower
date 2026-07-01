package br.com.toppower.erp_toppower.product.exception;

public class DuplicateProductCodeException extends RuntimeException {

    public DuplicateProductCodeException(String code) {
        super("Já existe um produto cadastrado com o código: " + code);
    }
}
