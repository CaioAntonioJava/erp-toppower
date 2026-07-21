package br.com.toppower.erp_toppower.payable.exception;

public class PayablePaymentNotFoundException extends RuntimeException {

    public PayablePaymentNotFoundException(Long paymentId, Long payableId) {
        super("Pagamento " + paymentId + " não encontrado para a conta a pagar "
                + payableId + ".");
    }
}