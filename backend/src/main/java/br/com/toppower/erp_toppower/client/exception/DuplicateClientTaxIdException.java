package br.com.toppower.erp_toppower.client.exception;

public class DuplicateClientTaxIdException extends RuntimeException {

    public DuplicateClientTaxIdException(String taxId) {
        super("Já existe um cliente cadastrado com o CPF/CNPJ: " + taxId);
    }
}
