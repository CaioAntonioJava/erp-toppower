package br.com.toppower.erp_toppower.profile.exception;

import java.util.UUID;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(UUID uuid) {
        super("Perfil não encontrado: " + uuid);
    }
}
