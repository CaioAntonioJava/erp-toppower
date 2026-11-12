package br.com.toppower.erp_toppower.contractfinances.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status de um lançamento do financeiro de contrato. O soft delete
 * apenas marca {@link #INATIVO} — lançamentos inativos permanecem no
 * histórico, mas não somam nos totais do controle.
 */
@Schema(name = "ContractFinanceEntryStatus", description = "Status do lançamento do financeiro de contrato.",
        allowableValues = {"ATIVO", "INATIVO"})
public enum ContractFinanceEntryStatus {
    /** Lançamento ativo — soma nos totais do controle. */
    ATIVO,
    /** Lançamento inativado (soft delete) — não soma nos totais. */
    INATIVO
}
