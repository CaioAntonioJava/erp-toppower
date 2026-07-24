package br.com.toppower.erp_toppower.stock.mapper;

import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.stock.entity.StockMovement;
import br.com.toppower.erp_toppower.stock.enums.MovementSource;
import br.com.toppower.erp_toppower.stock.enums.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link StockMovementMapper}.
 *
 * <p>Cobre toResponse com produto presente e nulo.</p>
 */
class StockMovementMapperTest {

    @Test
    void toResponse_comProduto_mapeiaCamposCorretamente() {
        StockMovement movement = new StockMovement();
        movement.setId(1L);
        movement.setProductId(10L);
        movement.setQuantityChange(new BigDecimal("5.0000"));
        movement.setStockBefore(new BigDecimal("10.0000"));
        movement.setStockAfter(new BigDecimal("15.0000"));
        movement.setType(MovementType.ENTRADA);
        movement.setSource(MovementSource.MANUAL);
        movement.setSourceId(100L);
        movement.setSourceNumber("NF-123");
        movement.setReason("Ajuste de estoque");
        movement.setReversed(false);
        movement.setReversalOfId(null);

        Product product = new Product();
        product.setId(10L);
        product.setName("Cabo de Aço");
        product.setCode("CABO-001");

        var response = StockMovementMapper.toResponse(movement, product);

        assertEquals(1L, response.id());
        assertEquals(10L, response.productId());
        assertEquals("Cabo de Aço", response.productName());
        assertEquals("CABO-001", response.productCode());
        assertEquals(new BigDecimal("5.0000"), response.quantityChange());
        assertEquals(new BigDecimal("10.0000"), response.stockBefore());
        assertEquals(new BigDecimal("15.0000"), response.stockAfter());
        assertEquals(MovementType.ENTRADA, response.type());
        assertEquals(MovementSource.MANUAL, response.source());
        assertEquals(100L, response.sourceId());
        assertEquals("NF-123", response.sourceNumber());
        assertEquals("Ajuste de estoque", response.reason());
        assertFalse(response.reversed());
        assertNull(response.reversalOfId());
    }

    @Test
    void toResponse_semProduto_nomeECodigoNulos() {
        StockMovement movement = new StockMovement();
        movement.setId(1L);
        movement.setProductId(10L);
        movement.setQuantityChange(BigDecimal.ONE);
        movement.setStockBefore(BigDecimal.ZERO);
        movement.setStockAfter(BigDecimal.ONE);
        movement.setType(MovementType.ENTRADA);
        movement.setSource(MovementSource.MANUAL);

        var response = StockMovementMapper.toResponse(movement, null);

        assertEquals(10L, response.productId());
        assertNull(response.productName());
        assertNull(response.productCode());
    }

    @Test
    void toResponse_movimentoEstornado_mapeiaReversal() {
        StockMovement movement = new StockMovement();
        movement.setId(2L);
        movement.setProductId(10L);
        movement.setQuantityChange(new BigDecimal("-5.0000"));
        movement.setStockBefore(new BigDecimal("15.0000"));
        movement.setStockAfter(new BigDecimal("10.0000"));
        movement.setType(MovementType.ESTORNO_SAIDA);
        movement.setSource(MovementSource.SALES_ORDER);
        movement.setSourceId(200L);
        movement.setSourceNumber("PV-001");
        movement.setReason("Estorno por cancelamento");
        movement.setReversed(false);
        movement.setReversalOfId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("Produto");

        var response = StockMovementMapper.toResponse(movement, product);

        assertEquals(MovementType.ESTORNO_SAIDA, response.type());
        assertEquals(MovementSource.SALES_ORDER, response.source());
        assertEquals(1L, response.reversalOfId());
        assertFalse(response.reversed());
    }
}
