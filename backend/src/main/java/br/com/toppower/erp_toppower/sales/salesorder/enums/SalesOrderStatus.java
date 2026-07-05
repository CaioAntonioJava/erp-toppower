package br.com.toppower.erp_toppower.sales.salesorder.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ciclo de vida de um pedido de venda.
 *
 * <p>Transições válidas (controladas pelo serviço):</p>
 * <ul>
 *   <li>{@link #ABERTO} → {@link #FINALIZADO};</li>
 *   <li>{@link #CANCELADO} é terminal e só pode ser alcançado a partir de
 *       {@link #ABERTO} (antes da finalização).</li>
 * </ul>
 *
 * <ul>
 *   <li>{@link #ABERTO} — pedido registrado, em andamento. Estado inicial.</li>
 *   <li>{@link #FINALIZADO} — pedido concluído. Estado terminal de sucesso.</li>
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