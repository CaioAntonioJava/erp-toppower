package br.com.toppower.erp_toppower.sales.quotation.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ciclo de vida de uma proposta comercial.
 *
 * <ul>
 *   <li>{@link #ATIVA} — proposta emitida e dentro do prazo de validade. Estado inicial.</li>
 *   <li>{@link #CONVERTIDA} — proposta que foi convertida em pedido de venda.</li>
 *   <li>{@link #CANCELADA} — proposta cancelada manualmente antes da conversão.</li>
 *   <li>{@link #EXPIRADA} — proposta cuja validade foi ultrapassada sem conversão.</li>
 * </ul>
 */
@Schema(name = "QuotationStatus", description = "Situação atual da proposta comercial.",
        allowableValues = {"ATIVA", "CONVERTIDA", "CANCELADA", "EXPIRADA"})
public enum QuotationStatus {
    ATIVA,
    CONVERTIDA,
    CANCELADA,
    EXPIRADA
}
