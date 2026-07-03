package br.com.toppower.erp_toppower.sales.quotation.exception;

import java.util.UUID;

/**
 * Lançada quando o {@code customerUuid} ou {@code companyUuid}
 * informado na proposta não corresponde a nenhum registro ativo.
 */
public class QuotationClientNotFoundException extends RuntimeException {

    public QuotationClientNotFoundException(UUID uuid, String type) {
        super("Cliente do tipo " + type + " não encontrado: " + uuid);
    }
}
