package br.com.toppower.erp_toppower.organization.exception;

public class DuplicateOrganizationCnpjException extends RuntimeException {
    public DuplicateOrganizationCnpjException(String cnpj) {
        super("Já existe uma Organization cadastrada com o CNPJ: " + cnpj);
    }
}