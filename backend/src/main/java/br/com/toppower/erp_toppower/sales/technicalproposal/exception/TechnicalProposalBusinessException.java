package br.com.toppower.erp_toppower.sales.technicalproposal.exception;

/**
 * Lançada para sinalizar violações de regras de negócio específicas do
 * ciclo de vida da proposta técnica, como:
 *
 * <ul>
 *   <li>Tentar alterar uma proposta já {@code CONCLUIDA}
 *       (estado terminal para edição);</li>
 *   <li>Tentar concluir uma proposta que não está {@code EM_ANDAMENTO};</li>
 *   <li>Persistir uma proposta sem ao menos um serviço ou produto.</li>
 * </ul>
 */
public class TechnicalProposalBusinessException extends RuntimeException {

    public TechnicalProposalBusinessException(String message) {
        super(message);
    }
}