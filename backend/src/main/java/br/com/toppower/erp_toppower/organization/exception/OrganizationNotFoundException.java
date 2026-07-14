package br.com.toppower.erp_toppower.organization.exception;

public class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException(Long id) {
        super("Organization não encontrada: " + id);
    }
}