package br.com.toppower.erp_toppower.sales.quotation.exception;

/**
 * Lançada quando o {@code customerId} ou {@code companyId}
 * informado na proposta não corresponde a nenhum registro ativo.
 */
public class QuotationClientNotFoundException extends RuntimeException {

    public QuotationClientNotFoundException(Long id, String type) {
        super("Cliente do tipo " + type + " não encontrado: " + id);
    }
}
