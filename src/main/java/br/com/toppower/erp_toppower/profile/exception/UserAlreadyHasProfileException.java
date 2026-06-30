package br.com.toppower.erp_toppower.profile.exception;

import java.util.UUID;

public class UserAlreadyHasProfileException extends RuntimeException {

    public UserAlreadyHasProfileException(UUID userId) {
        super("O usuário já possui um perfil cadastrado: " + userId);
    }
}
