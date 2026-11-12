package br.com.toppower.erp_toppower.contractfinances.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Natureza de um lançamento do financeiro de contrato:
 * {@link #RECEBIDO} representa dinheiro que entrou ligado ao contrato,
 * os demais valores representam categorias de gasto.
 *
 * <p>O rótulo em português exibido na UI é definido no frontend
 * ({@code CONTRACT_FINANCE_TYPE_LABELS}); aqui apenas os identificadores.</p>
 */
@Schema(name = "ContractFinanceEntryType",
        description = "Natureza do lançamento do financeiro de contrato (recebimento ou categoria de gasto).",
        allowableValues = {"RECEBIDO", "MATERIAL", "COMBUSTIVEL", "REFEICAO", "PEDAGIO",
                "TERCEIROS", "OUTROS", "SALARIOS"})
public enum ContractFinanceEntryType {
    /** Dinheiro recebido ligado ao contrato (entrada). */
    RECEBIDO,
    /** Gasto com materiais aplicados na obra do contrato. */
    MATERIAL,
    /** Gasto com combustível (frota/equipamentos da obra). */
    COMBUSTIVEL,
    /** Gasto com refeições/alimentação da equipe. */
    REFEICAO,
    /** Gasto com pedágios na logística da obra. */
    PEDAGIO,
    /** Pagamentos a terceiros ligados à execução do contrato. */
    TERCEIROS,
    /** Salários da equipe alocada ao contrato. */
    SALARIOS,
    /** Gasto diverso não categorizado acima. */
    OUTROS
}
