package br.com.toppower.erp_toppower.stock.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Origem de uma movimentação de estoque — identifica o módulo/dispositivo
 * de negócio que a gerou, habilitando rastreabilidade e estorno agrupado
 * por origem.
 *
 * <ul>
 *   <li>{@link #SALES_ORDER} — pedido de venda (saída ao finalizar,
 *       estorno ao cancelar um pedido finalizado);</li>
 *   <li>{@link #MANUAL} — ajuste manual direto no saldo (reservado para
 *       o endpoint de ajuste manual futuro).</li>
 * </ul>
 *
 * <p>Novas origens (compra, devolução de cliente, ajuste de inventário,
 * perda) devem ser adicionadas aqui — o {@code StockService} é agnóstico
 * à origem e opera apenas com os tipos de
 * {@link MovementType}.</p>
 */
@Schema(name = "MovementSource",
        description = "Módulo de origem da movimentação de estoque.",
        allowableValues = {"SALES_ORDER", "MANUAL"})
public enum MovementSource {
    SALES_ORDER,
    MANUAL
}