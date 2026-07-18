package br.com.toppower.erp_toppower.receivable.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Origem de uma conta a receber.
 *
 * <ul>
 *   <li>{@link #MANUAL} — cadastrada manualmente pelo usuário;</li>
 *   <li>{@link #SALES_ORDER} — gerada automaticamente ao converter uma
 *       proposta em pedido de venda;</li>
 *   <li>{@link #TECHNICAL_PROPOSAL} — gerada ao finalizar (concluir) uma
 *       proposta técnica;</li>
 *   <li>{@link #CONTRACT} — gerada ao finalizar (concluir) um contrato.</li>
 * </ul>
 */
@Schema(name = "ReceivableSource", description = "Origem da conta a receber.",
        allowableValues = {"MANUAL", "SALES_ORDER", "TECHNICAL_PROPOSAL", "CONTRACT"})
public enum ReceivableSource {
    /** Conta cadastrada manualmente pelo usuário. */
    MANUAL,
    /** Conta gerada ao converter uma proposta em pedido de venda. */
    SALES_ORDER,
    /** Conta gerada ao concluir uma proposta técnica. */
    TECHNICAL_PROPOSAL,
    /** Conta gerada ao concluir um contrato. */
    CONTRACT
}