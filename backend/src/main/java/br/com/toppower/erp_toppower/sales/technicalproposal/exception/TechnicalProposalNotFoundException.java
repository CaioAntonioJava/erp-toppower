package br.com.toppower.erp_toppower.sales.technicalproposal.exception;

/**
 * Lançada quando nenhuma proposta técnica é encontrada para o
 * identificador (ID) ou código informado.
 */
public class TechnicalProposalNotFoundException extends RuntimeException {

    public TechnicalProposalNotFoundException(Long id) {
        super("Proposta técnica não encontrada: " + id);
    }

    public TechnicalProposalNotFoundException(String code) {
        super("Proposta técnica não encontrada: " + code);
    }
}