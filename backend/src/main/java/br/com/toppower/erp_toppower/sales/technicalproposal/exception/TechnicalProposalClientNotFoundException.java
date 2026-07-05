package br.com.toppower.erp_toppower.sales.technicalproposal.exception;

import java.util.UUID;

/**
 * Lançada quando o {@code customerUuid} ou {@code companyUuid} informado
 * na proposta técnica não corresponde a nenhum registro ativo.
 */
public class TechnicalProposalClientNotFoundException extends RuntimeException {

    public TechnicalProposalClientNotFoundException(UUID uuid, String type) {
        super("Cliente do tipo " + type + " não encontrado: " + uuid);
    }
}