package br.com.toppower.erp_toppower.product.mapper;

import br.com.toppower.erp_toppower.product.dto.ProductCreateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductUpdateRequest;
import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.product.enums.OrigemProduto;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link ProductMapper}.
 *
 * <p>Cobre toEntity, toResponse e applyUpdate.</p>
 */
class ProductMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Cabo de Aço 1/4", "CABO-001", UnitType.UN, ProductStatus.ATIVO,
                new BigDecimal("150.00"), new BigDecimal("100.0000"),
                "73121010", OrigemProduto.NACIONAL, "7891234567890",
                "1234567", "99", new BigDecimal("0.5000"), new BigDecimal("0.5500"),
                "102", new BigDecimal("18.00"), new BigDecimal("50.00"),
                "99", "12345", "49", "49");

        Product result = ProductMapper.toEntity(request);

        assertEquals("Cabo de Aço 1/4", result.getName());
        assertEquals("CABO-001", result.getCode());
        assertEquals(UnitType.UN, result.getUnitType());
        assertEquals(ProductStatus.ATIVO, result.getStatus());
        assertEquals(new BigDecimal("150.00"), result.getPrice());
        assertEquals(new BigDecimal("100.0000"), result.getStockQuantity());
        assertEquals("73121010", result.getNcm());
        assertEquals(OrigemProduto.NACIONAL, result.getOrigem());
        assertEquals("7891234567890", result.getCodigoBarras());
        assertEquals("1234567", result.getCest());
        assertEquals("99", result.getExTipi());
        assertEquals(new BigDecimal("0.5000"), result.getPesoLiquido());
        assertEquals(new BigDecimal("0.5500"), result.getPesoBruto());
        assertEquals("102", result.getCsosn());
        assertEquals(new BigDecimal("18.00"), result.getAliquotaIcmsSt());
        assertEquals(new BigDecimal("50.00"), result.getMvaSt());
        assertEquals("99", result.getCstIpi());
        assertEquals("12345", result.getClasseEnqIpi());
        assertEquals("49", result.getCstPis());
        assertEquals("49", result.getCstCofins());
    }

    @Test
    void toEntity_camposOpcionaisNulos_mapeiaComoNull() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Produto Simples", null, UnitType.UN, null,
                new BigDecimal("10.00"), BigDecimal.ZERO,
                "73121010", null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        Product result = ProductMapper.toEntity(request);

        assertEquals("Produto Simples", result.getName());
        assertNull(result.getCode());
        assertNull(result.getStatus()); // @PrePersist aplica default
        assertNull(result.getOrigem());
        assertNull(result.getCodigoBarras());
        assertNull(result.getCest());
        assertNull(result.getExTipi());
        assertNull(result.getPesoLiquido());
        assertNull(result.getPesoBruto());
        assertNull(result.getCsosn());
        assertNull(result.getAliquotaIcmsSt());
        assertNull(result.getMvaSt());
        assertNull(result.getCstIpi());
        assertNull(result.getClasseEnqIpi());
        assertNull(result.getCstPis());
        assertNull(result.getCstCofins());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Produto Teste");
        product.setCode("PROD001");
        product.setUnitType(UnitType.KG);
        product.setStatus(ProductStatus.ATIVO);
        product.setPrice(new BigDecimal("99.90"));
        product.setStockQuantity(new BigDecimal("50.0000"));
        product.setNcm("73121010");

        var response = ProductMapper.toResponse(product);

        assertEquals(1L, response.id());
        assertEquals("Produto Teste", response.name());
        assertEquals("PROD001", response.code());
        assertEquals(UnitType.KG, response.unitType());
        assertEquals(ProductStatus.ATIVO, response.status());
        assertEquals(new BigDecimal("99.90"), response.price());
        assertEquals(new BigDecimal("50.0000"), response.stockQuantity());
        assertEquals("73121010", response.ncm());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Product product = new Product();
        product.setName("Original");
        product.setCode("ORIG");
        product.setUnitType(UnitType.UN);
        product.setStatus(ProductStatus.ATIVO);
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQuantity(BigDecimal.ZERO);
        product.setNcm("73121010");

        ProductUpdateRequest update = new ProductUpdateRequest(
                "Novo Nome", "NOVO", UnitType.KG, ProductStatus.INATIVO,
                new BigDecimal("20.00"), new BigDecimal("5.0000"),
                "73121011", OrigemProduto.ESTRANGEIRA_IMPORTACAO_DIRETA, "7890000000000",
                "7654321", "88", new BigDecimal("1.0000"), new BigDecimal("1.1000"),
                "201", new BigDecimal("12.00"), new BigDecimal("30.00"),
                "99", "54321", "49", "49");

        ProductMapper.applyUpdate(product, update);

        assertEquals("Novo Nome", product.getName());
        assertEquals("NOVO", product.getCode());
        assertEquals(UnitType.KG, product.getUnitType());
        assertEquals(ProductStatus.INATIVO, product.getStatus());
        assertEquals(new BigDecimal("20.00"), product.getPrice());
        assertEquals(new BigDecimal("5.0000"), product.getStockQuantity());
        assertEquals("73121011", product.getNcm());
        assertEquals(OrigemProduto.ESTRANGEIRA_IMPORTACAO_DIRETA, product.getOrigem());
        assertEquals("7890000000000", product.getCodigoBarras());
        assertEquals("7654321", product.getCest());
        assertEquals("88", product.getExTipi());
        assertEquals(new BigDecimal("1.0000"), product.getPesoLiquido());
        assertEquals(new BigDecimal("1.1000"), product.getPesoBruto());
        assertEquals("201", product.getCsosn());
        assertEquals(new BigDecimal("12.00"), product.getAliquotaIcmsSt());
        assertEquals(new BigDecimal("30.00"), product.getMvaSt());
        assertEquals("99", product.getCstIpi());
        assertEquals("54321", product.getClasseEnqIpi());
        assertEquals("49", product.getCstPis());
        assertEquals("49", product.getCstCofins());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Product product = new Product();
        product.setName("Original");
        product.setStatus(ProductStatus.ATIVO);

        ProductUpdateRequest update = new ProductUpdateRequest(
                null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        ProductMapper.applyUpdate(product, update);

        assertEquals("Original", product.getName());
        assertEquals(ProductStatus.ATIVO, product.getStatus());
    }
}
