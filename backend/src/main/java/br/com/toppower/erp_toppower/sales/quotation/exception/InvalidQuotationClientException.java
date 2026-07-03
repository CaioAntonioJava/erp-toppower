package br.com.toppower.erp_toppower.sales.quotation.exception;

/**
 * Lançada quando a referência ao cliente da proposta viola a invariante
 * "exatamente um entre customerUuid e companyUuid deve estar preenchido".
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *   <li>Ambos nulos — proposta sem cliente;</li>
 *   <li>Ambos preenchidos — ambiguidade na referência ao cliente.</li>
 * </ul>
 */
public class InvalidQuotationClientException extends RuntimeException {

    public InvalidQuotationClientException(String message) {
        super(message);
    }

    public static InvalidQuotationClientException bothNull() {
        return new InvalidQuotationClientException(
                "A proposta deve referenciar um cliente (pessoa física) ou uma empresa (pessoa jurídica).");
    }

    public static InvalidQuotationClientException bothSet() {
        return new InvalidQuotationClientException(
                "A proposta deve referenciar apenas um cliente OU uma empresa, nunca ambos.");
    }
}
