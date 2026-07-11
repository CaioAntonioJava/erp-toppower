package br.com.toppower.erp_toppower.contract.exception;

import java.util.UUID;

/**
 * Exceção lançada quando o {@code customerUuid} informado na criação
 * ou atualização de um contrato não corresponde a nenhum cliente
 * cadastrado. Mapeada para HTTP 404 pelo handler global.
 */
public class ContractCustomerNotFoundException extends RuntimeException {

    public ContractCustomerNotFoundException(UUID customerUuid) {
        super("Cliente não encontrado: " + customerUuid);
    }
}