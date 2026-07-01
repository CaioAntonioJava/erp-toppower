package br.com.toppower.erp_toppower.company.exception;

public class DuplicateCompanyCodeException extends RuntimeException {

    public DuplicateCompanyCodeException(String code) {
        super("Já existe uma empresa cadastrada com o código: " + code);
    }
}
