package br.com.toppower.erp_toppower.payable.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Origem de uma conta a pagar.
 *
 * <ul>
 *   <li>{@link #MANUAL} — cadastrada manualmente pelo usuário;</li>
 *   <li>{@link #BOLETO} — gerada automaticamente a partir de um boleto
 *       vinculado a um fornecedor;</li>
 *   <li>{@link #PURCHASE_INVOICE} — gerada a partir de uma nota de
 *       compra (NF-e XML) importada. <b>Reservado para uso futuro</b>:
 *       o campo e o tipo já existem para que a importação de XML não
 *       exija alteração de schema.</li>
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
     * Conta gerada a partir de uma nota de compra (NF-e XML) importada.
     * Reservado para uso futuro.
     */
    PURCHASE_INVOICE
}