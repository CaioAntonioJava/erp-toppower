package br.com.toppower.erp_toppower.user.exception;

import java.util.UUID;

/**
 * Lançada quando o admin tenta vincular um usuário a um tenant ao qual o
 * usuário já está vinculado. Evita duplicação do vínculo N:N.
 */
public class DuplicateUserTenantException extends RuntimeException {

    public DuplicateUserTenantException(UUID userUuid, UUID tenantUuid) {
        super("Usuário já está vinculado ao tenant informado (user=" + userUuid + ", tenant=" + tenantUuid + ")");
    }
}