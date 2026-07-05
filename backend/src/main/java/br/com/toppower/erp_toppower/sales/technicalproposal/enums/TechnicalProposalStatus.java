package br.com.toppower.erp_toppower.sales.technicalproposal.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ciclo de vida de uma proposta técnica.
 *
 * <ul>
 *   <li>{@link #ABERTA} — proposta cadastrada, aguardando início da execução. Estado inicial.</li>
 *   <li>{@link #EM_ANDAMENTO} — execução em curso pela equipe técnica.</li>
 *   <li>{@link #CONCLUIDA} — execução finalizada. A data de entrega
 *       ({@code delivery_date}) é preenchida automaticamente na transição
 *       para este estado.</li>
 * </ul>
 */
@Schema(name = "TechnicalProposalStatus",
        description = "Situação atual da proposta técnica.",
        allowableValues = {"ABERTA", "EM_ANDAMENTO", "CONCLUIDA"})
public enum TechnicalProposalStatus {
    ABERTA,
    EM_ANDAMENTO,
    CONCLUIDA
}