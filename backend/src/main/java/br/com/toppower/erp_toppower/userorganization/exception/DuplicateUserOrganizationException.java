package br.com.toppower.erp_toppower.userorganization.exception;

public class DuplicateUserOrganizationException extends RuntimeException {
    public DuplicateUserOrganizationException() {
        super("Este usuário já está vinculado a esta Organization.");
    }
}