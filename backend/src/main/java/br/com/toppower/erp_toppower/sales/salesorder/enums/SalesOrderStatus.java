package br.com.toppower.erp_toppower.sales.salesorder.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ciclo de vida de um pedido de venda.
 *
 * <p>Transições válidas (controladas pelo serviço):</p>
 * <ul>
 *   <li>{@link #ABERTO} → {@link #EM_SEPARACAO} → {@link #FATURADO} → {@link #ENTREGUE};</li>
 *   <li>{@link #CANCELADO} é terminal e só pode ser alcançado a partir de
 *       {@link #ABERTO} ou {@link #EM_SEPARACAO} (antes do faturamento).</li>
 * </ul>
 *
 * <ul>
 *   <li>{@link #ABERTO} — pedido registrado, aguardando separação no estoque. Estado inicial.</li>
 *   <li>{@link #EM_SEPARACAO} — separação/emissão de nota em andamento.</li>
 *   <li>{@link #FATURADO} — nota fiscal emitida, aguardando entrega.</li>
 *   <li>{@link #ENTREGUE} — mercadoria entregue. Estado terminal de sucesso.</li>
 *   <li>{@link #CANCELADO} — pedido cancelado (soft via status).</li>
 * </ul>
 */
@Schema(name = "SalesOrderStatus", description = "Situação atual do pedido de venda.",
        allowableValues = {"ABERTO", "EM_SEPARACAO", "FATURADO", "ENTREGUE", "CANCELADO"})
public enum SalesOrderStatus {
    ABERTO,
    EM_SEPARACAO,
    FATURADO,
    ENTREGUE,
    CANCELADO
}