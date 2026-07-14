package br.com.toppower.erp_toppower.contract.exception;

/**
 * Exceção lançada quando o {@code companyId} informado na criação ou
 * atualização de um contrato não corresponde a nenhuma empresa
 * cadastrada. Mapeada para HTTP 404 pelo handler global.
 */
public class ContractCompanyNotFoundException extends RuntimeException {

    public ContractCompanyNotFoundException(Long companyId) {
        super("Empresa não encontrada: " + companyId);
    }
}