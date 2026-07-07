package br.com.toppower.erp_toppower.organization.exception;

import java.util.UUID;

public class OrganizationInactiveException extends RuntimeException {
    public OrganizationInactiveException(UUID uuid) {
        super("A Organization informada está inativa e não pode ser usada como ativa: " + uuid);
    }
}