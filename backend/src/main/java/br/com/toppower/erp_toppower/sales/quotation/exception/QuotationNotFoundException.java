package br.com.toppower.erp_toppower.sales.quotation.exception;

import java.util.UUID;

/**
 * Lançada quando nenhuma proposta é encontrada para o identificador
 * (UUID ou número) informado.
 */
public class QuotationNotFoundException extends RuntimeException {

    public QuotationNotFoundException(UUID uuid) {
        super("Proposta não encontrada: " + uuid);
    }

    public QuotationNotFoundException(Long number) {
        super("Proposta não encontrada: " + number);
    }
}
