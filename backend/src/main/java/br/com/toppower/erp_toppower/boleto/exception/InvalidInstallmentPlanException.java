package br.com.toppower.erp_toppower.boleto.exception;

/**
 * Lançada quando os parâmetros de parcelamento de um boleto são
 * inválidos ou inconsistentes (ex.: quantidade de parcelas sem prazos,
 * prazos em quantidade divergente do número de parcelas, prazo inválido).
 */
public class InvalidInstallmentPlanException extends RuntimeException {

    public InvalidInstallmentPlanException(String message) {
        super(message);
    }
}