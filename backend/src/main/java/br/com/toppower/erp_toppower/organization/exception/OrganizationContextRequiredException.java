package br.com.toppower.erp_toppower.organization.exception;

/**
 * Lançada quando uma requisição autenticada (não-admin) não envia o header
 * {@code X-Organization-Id} e não é possível determinar a Organization ativa.
 */
public class OrganizationContextRequiredException extends RuntimeException {
    public OrganizationContextRequiredException() {
        super("Header 'X-Organization-Id' é obrigatório para esta operação.");
    }
}