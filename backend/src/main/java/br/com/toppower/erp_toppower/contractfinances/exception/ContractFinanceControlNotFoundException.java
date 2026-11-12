package br.com.toppower.erp_toppower.contractfinances.exception;

/** Controle de financeiro de contrato não encontrado para o identificador informado. */
public class ContractFinanceControlNotFoundException extends RuntimeException {

    public ContractFinanceControlNotFoundException(Long id) {
        super("Controle de financeiro de contrato não encontrado: " + id);
    }
}
