package br.com.toppower.erp_toppower.userorganization.exception;

import java.util.UUID;

public class UserOrganizationNotFoundException extends RuntimeException {
    public UserOrganizationNotFoundException(UUID uuid) {
        super("Vínculo usuário↔Organization não encontrado: " + uuid);
    }
}