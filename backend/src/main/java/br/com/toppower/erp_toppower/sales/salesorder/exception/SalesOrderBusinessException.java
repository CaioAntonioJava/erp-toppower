package br.com.toppower.erp_toppower.sales.salesorder.exception;

/**
 * Lançada para sinalizar violações de regras de negócio específicas do
 * ciclo de vida do pedido de venda, como:
 *
 * <ul>
 *   <li>Tentar alterar um pedido já {@code FINALIZADO} ou
 *       {@code CANCELADO};</li>
 *   <li>Tentar cancelar um pedido já {@code CANCELADO} ou já
 *       {@code FINALIZADO};</li>
 *   <li>Solicitar uma transição de status inválida;</li>
 *   <li>Persistir um pedido sem itens.</li>
 * </ul>
 */
public class SalesOrderBusinessException extends RuntimeException {

    public SalesOrderBusinessException(String message) {
        super(message);
    }
}