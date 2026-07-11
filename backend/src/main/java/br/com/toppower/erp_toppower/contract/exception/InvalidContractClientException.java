package br.com.toppower.erp_toppower.contract.exception;

/**
 * Lançada quando a referência ao cliente do contrato viola a invariante
 * "exatamente um entre customerUuid e companyUuid deve estar preenchido".
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *   <li>Ambos nulos — contrato sem cliente;</li>
 *   <li>Ambos preenchidos — ambiguidade na referência ao cliente.</li>
 * </ul>
 */
public class InvalidContractClientException extends RuntimeException {

    public InvalidContractClientException(String message) {
        super(message);
    }

    public static InvalidContractClientException bothNull() {
        return new InvalidContractClientException(
                "O contrato deve referenciar um cliente (pessoa física) "
                        + "ou uma empresa (pessoa jurídica).");
    }

    public static InvalidContractClientException bothSet() {
        return new InvalidContractClientException(
                "O contrato deve referenciar apenas um cliente OU uma "
                        + "empresa, nunca ambos.");
    }
}