package br.com.toppower.erp_toppower.sales.salesorder.exception;

/**
 * Lançada quando a referência ao cliente do pedido viola a invariante
 * "exatamente um entre customerUuid e companyUuid deve estar preenchido".
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *   <li>Ambos nulos — pedido sem cliente;</li>
 *   <li>Ambos preenchidos — ambiguidade na referência ao cliente.</li>
 * </ul>
 */
public class InvalidSalesOrderClientException extends RuntimeException {

    public InvalidSalesOrderClientException(String message) {
        super(message);
    }

    public static InvalidSalesOrderClientException bothNull() {
        return new InvalidSalesOrderClientException(
                "O pedido deve referenciar um cliente (pessoa física) ou uma empresa (pessoa jurídica).");
    }

    public static InvalidSalesOrderClientException bothSet() {
        return new InvalidSalesOrderClientException(
                "O pedido deve referenciar apenas um cliente OU uma empresa, nunca ambos.");
    }
}