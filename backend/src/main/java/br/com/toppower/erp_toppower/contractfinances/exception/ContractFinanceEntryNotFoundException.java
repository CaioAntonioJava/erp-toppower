package br.com.toppower.erp_toppower.contractfinances.exception;

/** Lançamento do financeiro de contrato não encontrado. */
public class ContractFinanceEntryNotFoundException extends RuntimeException {

    public ContractFinanceEntryNotFoundException(Long id) {
        super("Lançamento do financeiro de contrato não encontrado: " + id);
    }
}
