package br.com.toppower.erp_toppower.contract.exception;

/**
 * Lançada para sinalizar violações de regras de negócio específicas do
 * contrato, como a Organization ativa não possuir {@code contract_prefix}
 * configurado (impossibilitando a geração do código comercial).
 */
public class ContractBusinessException extends RuntimeException {

    public ContractBusinessException(String message) {
        super(message);
    }
}