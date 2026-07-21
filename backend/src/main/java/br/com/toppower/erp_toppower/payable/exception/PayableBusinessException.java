package br.com.toppower.erp_toppower.payable.exception;

/**
 * Violação de regra de negócio de conta a pagar (pagamento excede o
 * saldo da parcela, tentativa de pagar parcela cancelada, edição de
 * conta PAGO, etc.). Mapeada para HTTP 409 CONFLICT pelo
 * {@code GlobalExceptionHandler}.
 */
public class PayableBusinessException extends RuntimeException {

    public PayableBusinessException(String message) {
        super(message);
    }

    /**
     * Pagamento cujo valor ultrapassa o saldo devedor atual da parcela.
     */
    public static PayableBusinessException paymentExceedsInstallmentBalance(
            java.math.BigDecimal attempted, java.math.BigDecimal balance) {
        return new PayableBusinessException(
                "Pagamento de " + attempted + " excede o saldo da parcela ("
                        + balance + ").");
    }

    /**
     * Tentativa de registrar pagamento em parcela que não está ABERTO.
     */
    public static PayableBusinessException installmentNotOpenForPayment() {
        return new PayableBusinessException(
                "Apenas parcelas em ABERTO podem receber pagamentos. "
                        + "Reative a parcela antes de registrar um pagamento.");
    }

    /**
     * Tentativa de editar/cancelar conta em estado terminal (PAGO).
     */
    public static PayableBusinessException cannotModifyPaid() {
        return new PayableBusinessException(
                "Conta PAGO não pode ser alterada nem cancelada.");
    }

    /**
     * Tentativa de gerar conta a pagar a partir de um boleto que não
     * possui fornecedor vinculado.
     */
    public static PayableBusinessException boletoWithoutSupplier(Long boletoId) {
        return new PayableBusinessException(
                "Boleto " + boletoId + " não possui fornecedor vinculado. "
                        + "Vincule um supplier ao boleto antes de gerar a conta a pagar.");
    }

    /**
     * Tentativa de gerar conta a pagar a partir de um boleto que já
     * possui conta a pagar ativa vinculada.
     */
    public static PayableBusinessException boletoAlreadyLinked(Long boletoId) {
        return new PayableBusinessException(
                "Boleto " + boletoId + " já possui uma conta a pagar ativa vinculada.");
    }
}