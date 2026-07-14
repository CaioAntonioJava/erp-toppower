package br.com.toppower.erp_toppower.userorganization.exception;

public class UserOrganizationNotFoundException extends RuntimeException {
    public UserOrganizationNotFoundException(Long id) {
        super("Vínculo usuário↔Organization não encontrado: " + id);
    }
}