package br.com.toppower.erp_toppower.sales.salesorder.exception;

/**
 * Lançada ao tentar converter uma proposta em pedido de venda quando a
 * proposta já foi convertida anteriormente, ou não está mais em um
 * estado que permita conversão (ex.: CANCELADA, EXPIRADA).
 */
public class QuotationAlreadyConvertedException extends RuntimeException {

    public QuotationAlreadyConvertedException(Long quotationId) {
        super("Proposta " + quotationId + " não pode ser convertida (já convertida ou em estado inválido).");
    }
}