package br.com.toppower.erp_toppower.tenant.exception;

public class DuplicateTenantCnpjException extends RuntimeException {

    public DuplicateTenantCnpjException(String cnpj) {
        super("Já existe um tenant cadastrado com o CNPJ: " + cnpj);
    }
}