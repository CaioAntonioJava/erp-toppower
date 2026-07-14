package br.com.toppower.erp_toppower.organization.exception;

/**
 * Lançada quando o usuário autenticado NÃO possui acesso à Organization
 * informada no header {@code X-Organization-Id}.
 */
public class OrganizationAccessDeniedException extends RuntimeException {
    public OrganizationAccessDeniedException(Long organizationId) {
        super("Você não possui acesso à Organization: " + organizationId);
    }
}