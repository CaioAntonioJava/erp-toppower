package br.com.toppower.erp_toppower.profile.exception;

public class DuplicateProfileEmailException extends RuntimeException {

    public DuplicateProfileEmailException(String email) {
        super("Ja existe um perfil cadastrado com o e-mail: " + email);
    }
}
