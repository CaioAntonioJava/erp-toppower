package br.com.toppower.erp_toppower.company.exception;

public class DuplicateCompanyCnpjException extends RuntimeException {

    public DuplicateCompanyCnpjException(String cnpj) {
        super("Já existe uma empresa cadastrada com o CNPJ: " + cnpj);
    }
}
