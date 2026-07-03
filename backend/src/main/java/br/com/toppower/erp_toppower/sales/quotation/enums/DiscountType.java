package br.com.toppower.erp_toppower.sales.quotation.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Indica como o valor de um desconto deve ser interpretado.
 *
 * <ul>
 *   <li>{@link #AMOUNT} — valor monetário fixo em reais (ex.: {@code 150.00} → R$ 150,00 de desconto).</li>
 *   <li>{@link #PERCENT} — percentual aplicado sobre a base de cálculo (ex.: {@code 10.00} → 10% de desconto).</li>
 * </ul>
 */
@Schema(name = "DiscountType", description = "Tipo de aplicação de um desconto (valor fixo ou percentual).",
        allowableValues = {"AMOUNT", "PERCENT"})
public enum DiscountType {
    AMOUNT,
    PERCENT
}
