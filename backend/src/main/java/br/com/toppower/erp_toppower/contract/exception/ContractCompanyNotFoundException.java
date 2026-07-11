package br.com.toppower.erp_toppower.contract.exception;

import java.util.UUID;

/**
 * Exceção lançada quando o {@code companyUuid} informado na criação ou
 * atualização de um contrato não corresponde a nenhuma empresa
 * cadastrada. Mapeada para HTTP 404 pelo handler global.
 */
public class ContractCompanyNotFoundException extends RuntimeException {

    public ContractCompanyNotFoundException(UUID companyUuid) {
        super("Empresa não encontrada: " + companyUuid);
    }
}