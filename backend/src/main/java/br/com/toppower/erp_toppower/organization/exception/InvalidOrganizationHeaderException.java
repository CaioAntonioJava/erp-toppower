package br.com.toppower.erp_toppower.organization.exception;

/**
 * Lançada quando o header {@code X-Organization-Id} está presente mas com
 * valor inválido (não é um UUID válido).
 */
public class InvalidOrganizationHeaderException extends RuntimeException {
    public InvalidOrganizationHeaderException(String value) {
        super("Header 'X-Organization-Id' inválido (UUID esperado): " + value);
    }
}