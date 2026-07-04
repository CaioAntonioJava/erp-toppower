package br.com.toppower.erp_toppower.sales.quotation.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Indica quem é responsável pelo pagamento do frete da proposta.
 *
 * <ul>
 *   <li>{@link #CIF} — "Cost, Insurance and Freight": frete por conta do
 *       remetente (embutido no valor da proposta).</li>
 *   <li>{@link #FOB} — "Free On Board": frete por conta do destinatário
 *       (pago no recebimento).</li>
 * </ul>
 */
@Schema(name = "FreightType", description = "Tipo de frete (quem paga o frete).",
        allowableValues = {"CIF", "FOB"})
public enum FreightType {
    CIF,
    FOB
}