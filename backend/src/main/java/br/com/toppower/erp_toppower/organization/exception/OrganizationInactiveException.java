package br.com.toppower.erp_toppower.organization.exception;

public class OrganizationInactiveException extends RuntimeException {
    public OrganizationInactiveException(Long id) {
        super("A Organization informada está inativa e não pode ser usada como ativa: " + id);
    }
}