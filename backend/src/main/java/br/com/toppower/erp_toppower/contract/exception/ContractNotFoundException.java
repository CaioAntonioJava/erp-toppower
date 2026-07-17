package br.com.toppower.erp_toppower.contract.exception;

public class ContractNotFoundException extends RuntimeException {

    public ContractNotFoundException(Long id) {
        super("Contrato não encontrado: " + id);
    }
}