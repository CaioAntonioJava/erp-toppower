package br.com.toppower.erp_toppower.customer.exception;

public class DuplicateCustomerCodeException extends RuntimeException {

    public DuplicateCustomerCodeException(String code) {
        super("Já existe um cliente (pessoa física) cadastrado com o código: " + code);
    }
}
