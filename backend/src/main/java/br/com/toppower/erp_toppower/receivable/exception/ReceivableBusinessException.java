package br.com.toppower.erp_toppower.receivable.exception;

/**
 * Violação de regra de negócio de conta a receber (pagamento excede o
 * saldo devedor, tentativa de pagar conta cancelada, reabertura de
 * documento com pagamentos registrados, etc.). Mapeda para HTTP 409
 * CONFLICT pelo {@code GlobalExceptionHandler}.
 */
public class ReceivableBusinessException extends RuntimeException {

    public ReceivableBusinessException(String message) {
        super(message);
    }

    /**
     * Pagamento cujo valor ultrapassa o saldo devedor atual da conta.
     */
    public static ReceivableBusinessException paymentExceedsBalance(
            java.math.BigDecimal attempted, java.math.BigDecimal balance) {
        return new ReceivableBusinessException(
                "Pagamento de " + attempted + " excede o saldo devedor atual ("
                        + balance + ").");
    }

    /**
     * Tentativa de registrar pagamento em conta que não está ABERTO.
     */
    public static ReceivableBusinessException notOpenForPayment() {
        return new ReceivableBusinessException(
                "Apenas contas em ABERTO podem receber pagamentos. "
                        + "Reative a conta antes de registrar um pagamento.");
    }

    /**
     * Tentativa de editar/cancelar conta em estado terminal (PAGO).
     */
    public static ReceivableBusinessException cannotModifyPaid() {
        return new ReceivableBusinessException(
                "Conta PAGO não pode ser alterada nem cancelada. "
                        + "Reabra/manter conforme necessário.");
    }

    /**
     * Reabertura de documento de origem bloqueada porque a conta
     * vinculada já possui pagamentos registrados.
     */
    public static ReceivableBusinessException cannotReopenWithPayments(String documentLabel) {
        return new ReceivableBusinessException(
                "Não é possível reabrir " + documentLabel + ": a conta a receber "
                        + "vinculada já possui pagamentos registrados. Trate a conta "
                        + "manualmente no módulo de Contas a Receber antes de reabrir.");
    }
}