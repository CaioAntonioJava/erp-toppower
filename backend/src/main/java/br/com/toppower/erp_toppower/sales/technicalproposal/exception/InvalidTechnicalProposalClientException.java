package br.com.toppower.erp_toppower.sales.technicalproposal.exception;

/**
 * Lançada quando a referência ao cliente da proposta técnica viola a
 * invariante "exatamente um entre customerUuid e companyUuid deve estar
 * preenchido".
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *   <li>Ambos nulos — proposta sem cliente;</li>
 *   <li>Ambos preenchidos — ambiguidade na referência ao cliente.</li>
 * </ul>
 */
public class InvalidTechnicalProposalClientException extends RuntimeException {

    public InvalidTechnicalProposalClientException(String message) {
        super(message);
    }

    public static InvalidTechnicalProposalClientException bothNull() {
        return new InvalidTechnicalProposalClientException(
                "A proposta técnica deve referenciar um cliente (pessoa física) "
                        + "ou uma empresa (pessoa jurídica).");
    }

    public static InvalidTechnicalProposalClientException bothSet() {
        return new InvalidTechnicalProposalClientException(
                "A proposta técnica deve referenciar apenas um cliente OU uma "
                        + "empresa, nunca ambos.");
    }
}