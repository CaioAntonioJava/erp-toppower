package br.com.toppower.erp_toppower.cashflow.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documento de origem de um movimento do fluxo de caixa. Unifica os
 * enums {@code ReceivableSource} (contas a receber) e
 * {@code PayableSource} (contas a pagar) — os nomes coincidem nos
 * valores comuns, permitindo o mapeamento direto via
 * {@code valueOf}.
 */
@Schema(name = "CashFlowDocumentSource", description = "Documento de origem do movimento de caixa.",
        allowableValues = {"MANUAL", "SALES_ORDER", "TECHNICAL_PROPOSAL", "CONTRACT", "BOLETO", "PURCHASE_INVOICE"})
public enum CashFlowDocumentSource {
    /** Movimento de conta cadastrada manualmente (entrada ou saída). */
    MANUAL,
    /** Entrada gerada por pedido de venda (conta a receber). */
    SALES_ORDER,
    /** Entrada gerada por proposta técnica (conta a receber). */
    TECHNICAL_PROPOSAL,
    /** Entrada gerada por contrato (conta a receber). */
    CONTRACT,
    /** Saída gerada por boleto vinculado a fornecedor (conta a pagar). */
    BOLETO,
    /** Saída gerada por nota de compra (NF-e XML) importada (conta a pagar). */
    PURCHASE_INVOICE
}