package br.com.toppower.erp_toppower.contract.exception;

/**
 * Exceção lançada quando o {@code customerId} informado na criação
 * ou atualização de um contrato não corresponde a nenhum cliente
 * cadastrado. Mapeada para HTTP 404 pelo handler global.
 */
public class ContractCustomerNotFoundException extends RuntimeException {

    public ContractCustomerNotFoundException(Long customerId) {
        super("Cliente não encontrado: " + customerId);
    }
}