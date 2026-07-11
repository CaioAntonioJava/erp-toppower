package br.com.toppower.erp_toppower.organization.exception;

/**
 * Sinaliza tentativa de cadastrar/atualizar uma Organization com um prefixo
 * de Contrato já em uso por outra Organization. Mapeada para HTTP 409
 * pelo handler global de exceções.
 */
public class DuplicateOrganizationContractPrefixException extends RuntimeException {
    public DuplicateOrganizationContractPrefixException(String contractPrefix) {
        super("Já existe uma Organization cadastrada com o prefixo de contrato: " + contractPrefix);
    }
}