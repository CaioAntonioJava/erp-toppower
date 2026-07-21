package br.com.toppower.erp_toppower.payable.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status do ciclo de vida de uma conta a pagar (ou de uma parcela).
 *
 * <ul>
 *   <li>{@link #ABERTO} — conta/parcela em aberto, aguardando pagamento
 *       (total ou parcial);</li>
 *   <li>{@link #PAGO} — conta/parcela quitada (soma dos pagamentos
 *       alcançou o valor total);</li>
 *   <li>{@link #CANCELADO} — conta/parcela cancelada (soft delete da
 *       conta pai ou cancelamento individual da parcela).</li>
 * </ul>
 */
@Schema(name = "PayableStatus", description = "Status da conta a pagar (ou parcela).",
        allowableValues = {"ABERTO", "PAGO", "CANCELADO"})
public enum PayableStatus {
    /** Conta/parcela em aberto, aguardando pagamento. */
    ABERTO,
    /** Conta/parcela quitada (pagamentos somaram o valor total). */
    PAGO,
    /** Conta/parcela cancelada (soft delete). */
    CANCELADO
}