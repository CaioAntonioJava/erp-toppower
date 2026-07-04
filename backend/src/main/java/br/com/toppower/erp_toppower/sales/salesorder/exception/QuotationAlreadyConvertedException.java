package br.com.toppower.erp_toppower.sales.salesorder.exception;

import java.util.UUID;

/**
 * Lançada ao tentar converter uma proposta em pedido de venda quando a
 * proposta já foi convertida anteriormente, ou não está mais em um
 * estado que permita conversão (ex.: CANCELADA, EXPIRADA).
 */
public class QuotationAlreadyConvertedException extends RuntimeException {

    public QuotationAlreadyConvertedException(UUID quotationUuid) {
        super("Proposta " + quotationUuid + " não pode ser convertida (já convertida ou em estado inválido).");
    }
}