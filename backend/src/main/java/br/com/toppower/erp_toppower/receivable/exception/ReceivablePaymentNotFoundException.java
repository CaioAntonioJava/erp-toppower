package br.com.toppower.erp_toppower.receivable.exception;

public class ReceivablePaymentNotFoundException extends RuntimeException {

    public ReceivablePaymentNotFoundException(Long paymentId, Long receivableId) {
        super("Pagamento " + paymentId + " não encontrado para a conta a receber "
                + receivableId + ".");
    }
}