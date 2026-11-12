package br.com.toppower.erp_toppower.contractfinances.exception;

/**
 * Regra de negócio violada no financeiro de contratos — tipicamente
 * tentativa de lançar gasto/recebimento em um contrato INATIVO
 * (cancelado/excluído). Mapeada para 422 pelo GlobalExceptionHandler.
 */
public class InvalidContractFinanceContractException extends RuntimeException {

    public InvalidContractFinanceContractException(String message) {
        super(message);
    }

    public static InvalidContractFinanceContractException contractInactive(Long contractId) {
        return new InvalidContractFinanceContractException(
                "O contrato " + contractId + " está INATIVO e não aceita novos lançamentos.");
    }
}
