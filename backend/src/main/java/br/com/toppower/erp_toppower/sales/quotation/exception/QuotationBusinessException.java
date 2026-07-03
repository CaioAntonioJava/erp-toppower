package br.com.toppower.erp_toppower.sales.quotation.exception;

/**
 * Lançada para sinalizar violações de regras de negócio específicas
 * do ciclo de vida da proposta, como:
 *
 * <ul>
 *   <li>Tentar alterar uma proposta que já foi {@code CONVERTIDA} em
 *       pedido (estado terminal para edição);</li>
 *   <li>Tentar cancelar uma proposta que já está {@code CANCELADA};</li>
 *   <li>Persistir uma proposta sem itens.</li>
 * </ul>
 */
public class QuotationBusinessException extends RuntimeException {

    public QuotationBusinessException(String message) {
        super(message);
    }
}
