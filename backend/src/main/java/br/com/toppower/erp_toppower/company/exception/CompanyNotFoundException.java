package br.com.toppower.erp_toppower.company.exception;

import java.util.UUID;

public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException(UUID uuid) {
        super("Empresa não encontrada: " + uuid);
    }
}
