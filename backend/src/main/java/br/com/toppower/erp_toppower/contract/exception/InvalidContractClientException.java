package br.com.toppower.erp_toppower.contract.exception;

/**
 * Lançada quando a referência a cliente (PF ou PJ) é inválida:
 * ambos nulos ou ambos preenchidos.
 */
public class InvalidContractClientException extends RuntimeException {

    private InvalidContractClientException(String message) {
        super(message);
    }

    public static InvalidContractClientException bothNull() {
        return new InvalidContractClientException(
                "O contrato deve referenciar um cliente (pessoa física) ou uma empresa (pessoa jurídica).");
    }

    public static InvalidContractClientException bothSet() {
        return new InvalidContractClientException(
                "O contrato deve referenciar apenas um cliente OU uma empresa, nunca ambos.");
    }
}