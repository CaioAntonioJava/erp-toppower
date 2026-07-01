package br.com.toppower.erp_toppower.customer.exception;

public class DuplicateCustomerCpfException extends RuntimeException {

    public DuplicateCustomerCpfException(String cpf) {
        super("Já existe um cliente (pessoa física) cadastrado com o CPF: " + cpf);
    }
}
