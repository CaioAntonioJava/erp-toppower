package br.com.toppower.erp_toppower.payable.exception;

/**
 * Lançada quando uma parcela de conta a pagar não é encontrada, ou não
 * pertence à conta informada na rota.
 */
public class PayableInstallmentNotFoundException extends RuntimeException {

    public PayableInstallmentNotFoundException(Long installmentId, Long payableId) {
        super("Parcela " + installmentId + " não encontrada para a conta a pagar "
                + payableId + ".");
    }
}