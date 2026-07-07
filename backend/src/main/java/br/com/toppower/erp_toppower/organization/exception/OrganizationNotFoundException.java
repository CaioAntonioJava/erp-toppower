package br.com.toppower.erp_toppower.organization.exception;

import java.util.UUID;

public class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException(UUID uuid) {
        super("Organization não encontrada: " + uuid);
    }
}