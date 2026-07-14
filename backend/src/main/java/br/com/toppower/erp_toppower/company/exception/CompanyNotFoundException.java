package br.com.toppower.erp_toppower.company.exception;

public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException(Long id) {
        super("Empresa não encontrada: " + id);
    }
}
