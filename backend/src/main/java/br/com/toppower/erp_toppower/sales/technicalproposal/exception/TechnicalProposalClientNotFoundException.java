package br.com.toppower.erp_toppower.sales.technicalproposal.exception;

/**
 * Lançada quando o {@code customerId} ou {@code companyId} informado
 * na proposta técnica não corresponde a nenhum registro ativo.
 */
public class TechnicalProposalClientNotFoundException extends RuntimeException {

    public TechnicalProposalClientNotFoundException(Long id, String type) {
        super("Cliente do tipo " + type + " não encontrado: " + id);
    }
}