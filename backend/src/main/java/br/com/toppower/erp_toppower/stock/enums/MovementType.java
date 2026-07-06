package br.com.toppower.erp_toppower.stock.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tipo de movimentação de estoque registrada no diário
 * ({@code stock_movements}).
 *
 * <p>O sinal de {@code quantityChange} em {@code StockMovement} segue o tipo:</p>
 * <ul>
 *   <li>{@link #ENTRADA} — valor positivo (aumenta o saldo);</li>
 *   <li>{@link #SAIDA} — valor negativo (reduz o saldo);</li>
 *   <li>{@link #ESTORNO_SAIDA} — valor positivo (devolve uma saída anterior,
 *       ex.: cancelamento de pedido finalizado);</li>
 *   <li>{@link #ESTORNO_ENTRADA} — valor negativo (desfaz uma entrada
 *       anterior). Reservado para uso futuro.</li>
 * </ul>
 *
 * <p>Estornos sempre referenciam a movimentação original via
 * {@code reversalOfUuid} e marcam a original como {@code reversed=true},
 * garantindo que uma mesma origem não seja estornada duas vezes.</p>
 */
@Schema(name = "MovementType",
        description = "Tipo de movimentação de estoque.",
        allowableValues = {"ENTRADA", "SAIDA", "ESTORNO_SAIDA", "ESTORNO_ENTRADA"})
public enum MovementType {
    ENTRADA,
    SAIDA,
    ESTORNO_SAIDA,
    ESTORNO_ENTRADA
}