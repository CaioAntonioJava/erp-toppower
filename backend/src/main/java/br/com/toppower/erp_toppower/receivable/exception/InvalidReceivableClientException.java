package br.com.toppower.erp_toppower.receivable.exception;

/**
 * Lançada quando a referência a cliente (PF ou PJ) é inválida:
 * ambos nulos ou ambos preenchidos.
 */
public class InvalidReceivableClientException extends RuntimeException {

    private InvalidReceivableClientException(String message) {
        super(message);
    }

    public static InvalidReceivableClientException bothNull() {
        return new InvalidReceivableClientException(
                "A conta a receber deve referenciar um cliente (pessoa física) "
                        + "ou uma empresa (pessoa jurídica).");
    }

    public static InvalidReceivableClientException bothSet() {
        return new InvalidReceivableClientException(
                "A conta a receber deve referenciar apenas um cliente OU uma "
                        + "empresa, nunca ambos.");
    }
}