package br.com.toppower.erp_toppower.payable.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Origem de uma conta a pagar.
 *
 * <ul>
 *   <li>{@link #MANUAL} — cadastrada manualmente pelo usuário;</li>
 *   <li>{@link #BOLETO} — gerada automaticamente a partir de um boleto
 *       vinculado a um fornecedor;</li>
 *   <li>{@link #PURCHASE_INVOICE} — gerada automaticamente pela
 *       importação de NF-e XML (módulo purchase), com parcelas das
 *       duplicatas da nota (ou parcela única à vista).</li>
 * </ul>
 */
@Schema(name = "PayableSource", description = "Origem da conta a pagar.",
        allowableValues = {"MANUAL", "BOLETO", "PURCHASE_INVOICE"})
public enum PayableSource {
    /** Conta cadastrada manualmente pelo usuário. */
    MANUAL,
    /** Conta gerada a partir de um boleto vinculado a um fornecedor. */
    BOLETO,
    /**
     * Conta gerada pela importação de uma nota de compra (NF-e XML),
     * com parcelas das duplicatas da nota.
     */
    PURCHASE_INVOICE
}