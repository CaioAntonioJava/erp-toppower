package br.com.toppower.erp_toppower.user.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID uuid) {
        super("Usuário não encontrado: " + uuid);
    }
}
