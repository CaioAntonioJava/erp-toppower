package br.com.toppower.erp_toppower.cashflow.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tipo de movimento do fluxo de caixa consolidado.
 *
 * <ul>
 *   <li>{@link #ENTRADA} — dinheiro que entrou no caixa (pagamento
 *       recebido em uma conta a receber);</li>
 *   <li>{@link #SAIDA} — dinheiro que saiu do caixa (pagamento realizado
 *       em uma conta a pagar).</li>
 * </ul>
 */
@Schema(name = "CashFlowEntryType", description = "Tipo de movimento do fluxo de caixa.",
        allowableValues = {"ENTRADA", "SAIDA"})
public enum CashFlowEntryType {
    /** Entrada de caixa: pagamento registrado em contas a receber. */
    ENTRADA,
    /** Saída de caixa: pagamento registrado em contas a pagar. */
    SAIDA
}