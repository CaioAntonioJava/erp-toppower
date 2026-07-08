package br.com.toppower.erp_toppower.organization.exception;

/**
 * Sinaliza tentativa de cadastrar/atualizar uma Organization com um prefixo
 * de Proposta Técnica já em uso por outra Organization. Mapeada para HTTP
 * 409 pelo handler global de exceções.
 */
public class DuplicateOrganizationProposalPrefixException extends RuntimeException {
    public DuplicateOrganizationProposalPrefixException(String proposalPrefix) {
        super("Já existe uma Organization cadastrada com o prefixo de proposta: " + proposalPrefix);
    }
}
