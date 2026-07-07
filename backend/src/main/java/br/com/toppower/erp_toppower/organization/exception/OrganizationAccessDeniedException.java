package br.com.toppower.erp_toppower.organization.exception;

import java.util.UUID;

/**
 * Lançada quando o usuário autenticado NÃO possui acesso à Organization
 * informada no header {@code X-Organization-Id}.
 */
public class OrganizationAccessDeniedException extends RuntimeException {
    public OrganizationAccessDeniedException(UUID organizationId) {
        super("Você não possui acesso à Organization: " + organizationId);
    }
}