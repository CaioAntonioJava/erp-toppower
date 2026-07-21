package br.com.toppower.erp_toppower.receivable.exception;

/**
 * Lançada quando uma parcela de conta a receber não é encontrada, ou
 * não pertence à conta informada na rota.
 */
public class ReceivableInstallmentNotFoundException extends RuntimeException {

    public ReceivableInstallmentNotFoundException(Long installmentId, Long receivableId) {
        super("Parcela " + installmentId + " não encontrada para a conta a receber "
                + receivableId + ".");
    }
}