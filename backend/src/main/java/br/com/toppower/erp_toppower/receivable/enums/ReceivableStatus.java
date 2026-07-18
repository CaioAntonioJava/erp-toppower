package br.com.toppower.erp_toppower.receivable.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status do ciclo de vida de uma conta a receber.
 *
 * <ul>
 *   <li>{@link #ABERTO} — conta em aberto, aguardando recebimento
 *       (total ou parcial);</li>
 *   <li>{@link #PAGO} — conta quitada (soma dos pagamentos alcançou o
 *       valor total);</li>
 *   <li>{@link #CANCELADO} — conta cancelada (soft delete ou documento
 *       de origem reaberto sem pagamentos registrados).</li>
 * </ul>
 */
@Schema(name = "ReceivableStatus", description = "Status da conta a receber.",
        allowableValues = {"ABERTO", "PAGO", "CANCELADO"})
public enum ReceivableStatus {
    /** Conta em aberto, aguardando recebimento. */
    ABERTO,
    /** Conta quitada (pagamentos somaram o valor total). */
    PAGO,
    /** Conta cancelada (soft delete ou reabertura do documento de origem). */
    CANCELADO
}