package br.com.toppower.erp_toppower.stock.mapper;

import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.stock.dto.StockMovementResponse;
import br.com.toppower.erp_toppower.stock.entity.StockMovement;

/**
 * Conversões de {@link StockMovement} para {@link StockMovementResponse}.
 *
 * <p>{@code productName} e {@code productCode} são resolvidos pelo
 * chamador a partir do {@link Product} referenciado (não há snapshot
 * persistido), espelhando o padrão de {@code SalesOrderMapper} para
 * {@code clientName}.</p>
 */
public final class StockMovementMapper {

    private StockMovementMapper() {
    }

    /**
     * Monta a resposta a partir da movimentação e do produto resolvido.
     *
     * @param movement movimentação persistida
     * @param product  produto referenciado (pode ser {@code null} quando
     *                 não existe mais — nome/código ficam {@code null})
     */
    public static StockMovementResponse toResponse(StockMovement movement, Product product) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProductId(),
                product != null ? product.getName() : null,
                product != null ? product.getCode() : null,
                movement.getQuantityChange(),
                movement.getStockBefore(),
                movement.getStockAfter(),
                movement.getType(),
                movement.getSource(),
                movement.getSourceId(),
                movement.getSourceNumber(),
                movement.getReason(),
                movement.isReversed(),
                movement.getReversalOfId(),
                movement.getCreatedAt(),
                movement.getCreatedBy()
        );
    }
}