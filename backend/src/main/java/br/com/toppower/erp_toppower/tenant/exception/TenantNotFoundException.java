package br.com.toppower.erp_toppower.tenant.exception;

import java.util.UUID;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(UUID uuid) {
        super("Tenant não encontrado: " + uuid);
    }
}