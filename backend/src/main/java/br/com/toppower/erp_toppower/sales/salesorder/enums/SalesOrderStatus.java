package br.com.toppower.erp_toppower.sales.salesorder.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ciclo de vida de um pedido de venda.
 *
 * <p>Transições válidas (controladas pelo serviço):</p>
 * <ul>
 *   <li>{@link #ABERTO} → {@link #FINALIZADO};</li>
 *   <li>{@link #ABERTO} → {@link #CANCELADO} (antes da finalização —
 *       sem efeito sobre o estoque);</li>
 *   <li>{@link #FINALIZADO} → {@link #CANCELADO} (cancelamento com
 *       estorno automático das saídas de estoque registradas no
 *       finalizar).</li>
 * </ul>
 *
 * <ul>
 *   <li>{@link #ABERTO} — pedido registrado, em andamento. Estado inicial.</li>
 *   <li>{@link #FINALIZADO} — pedido concluído; o estoque dos itens foi
 *       baixado. Ainda pode ser cancelado (com estorno do estoque).</li>
 *   <li>{@link #CANCELADO} — pedido cancelado (soft via status).</li>
 * </ul>
 */
@Schema(name = "SalesOrderStatus", description = "Situação atual do pedido de venda.",
        allowableValues = {"ABERTO", "FINALIZADO", "CANCELADO"})
public enum SalesOrderStatus {
    ABERTO,
    FINALIZADO,
    CANCELADO
}