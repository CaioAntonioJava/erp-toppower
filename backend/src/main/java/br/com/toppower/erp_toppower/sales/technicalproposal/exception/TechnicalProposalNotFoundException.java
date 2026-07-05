package br.com.toppower.erp_toppower.sales.technicalproposal.exception;

import java.util.UUID;

/**
 * Lançada quando nenhuma proposta técnica é encontrada para o
 * identificador (UUID) ou código informado.
 */
public class TechnicalProposalNotFoundException extends RuntimeException {

    public TechnicalProposalNotFoundException(UUID uuid) {
        super("Proposta técnica não encontrada: " + uuid);
    }

    public TechnicalProposalNotFoundException(String code) {
        super("Proposta técnica não encontrada: " + code);
    }
}