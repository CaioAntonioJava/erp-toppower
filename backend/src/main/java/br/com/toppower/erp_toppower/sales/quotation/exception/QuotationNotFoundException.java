package br.com.toppower.erp_toppower.sales.quotation.exception;

/**
 * Lançada quando nenhuma proposta é encontrada para o identificador
 * (ID ou número) informado.
 */
public class QuotationNotFoundException extends RuntimeException {

    public QuotationNotFoundException(Long id) {
        super("Proposta não encontrada: " + id);
    }
}
