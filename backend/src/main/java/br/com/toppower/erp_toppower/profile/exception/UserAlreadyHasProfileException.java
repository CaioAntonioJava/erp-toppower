package br.com.toppower.erp_toppower.profile.exception;

public class UserAlreadyHasProfileException extends RuntimeException {

    public UserAlreadyHasProfileException(Long userId) {
        super("O usuário já possui um perfil cadastrado: " + userId);
    }
}
