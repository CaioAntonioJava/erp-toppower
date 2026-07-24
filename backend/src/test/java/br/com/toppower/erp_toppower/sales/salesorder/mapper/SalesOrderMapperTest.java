package br.com.toppower.erp_toppower.sales.salesorder.mapper;

import br.com.toppower.erp_toppower.sales.quotation.entity.Quotation;
import br.com.toppower.erp_toppower.sales.quotation.entity.QuotationItem;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderCreateRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderFromQuotationRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderItemRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderUpdateRequest;
import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrder;
import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link SalesOrderMapper}.
 *
 * <p>Cobre toEntity, toItemEntity, fromQuotation, fromQuotationItem,
 * applyUpdate, effectiveMargin, applyProfitMargin e calculateItemTotalPrice.</p>
 */
class SalesOrderMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        SalesOrderCreateRequest request = new SalesOrderCreateRequest(
                1L, null, "João", 10L,
                List.of(new SalesOrderItemRequest(100L, new BigDecimal("2"), new BigDecimal("100.00"), null)),
                DiscountType.PERCENT, new BigDecimal("5.00"),
                PaymentCondition.PRAZO_30_DIAS, "Observação",
                FreightType.CIF, new BigDecimal("20.00"), 5L, new BigDecimal("10.00"));

        SalesOrder result = SalesOrderMapper.toEntity(request);

        assertEquals(1L, result.getCustomerId());
        assertNull(result.getCompanyId());
        assertEquals("João", result.getAttention());
        assertEquals(10L, result.getSellerId());
        assertEquals(DiscountType.PERCENT, result.getDiscountType());
        assertEquals(new BigDecimal("5.00"), result.getDiscount());
        assertEquals(PaymentCondition.PRAZO_30_DIAS, result.getPaymentCondition());
        assertEquals("Observação", result.getNotes());
        assertEquals(FreightType.CIF, result.getFreightType());
        assertEquals(new BigDecimal("20.00"), result.getFreightValue());
        assertEquals(5L, result.getCarrierId());
        assertEquals(new BigDecimal("10.00"), result.getProfitMargin());
    }

    @Test
    void toItemEntity_semMargemPropria_usaMargemDoCabecalho() {
        SalesOrderItemRequest request = new SalesOrderItemRequest(
                100L, new BigDecimal("2"), new BigDecimal("100.00"), null);

        SalesOrderItem result = SalesOrderMapper.toItemEntity(request, 1L, new BigDecimal("10.00"));

        assertEquals(1L, result.getSalesOrderId());
        assertEquals(100L, result.getProductId());
        assertEquals(new BigDecimal("2"), result.getQuantity());
        assertEquals(new BigDecimal("100.00"), result.getBaseUnitPrice());
        // unitPrice = 100 * (1 + 10/100) = 110.00
        assertEquals(new BigDecimal("110.00"), result.getUnitPrice());
        assertNull(result.getProfitMargin());
        // totalPrice = 110 * 2 = 220.00
        assertEquals(new BigDecimal("220.00"), result.getTotalPrice());
    }

    @Test
    void toItemEntity_comMargemPropria_usaMargemDoItem() {
        SalesOrderItemRequest request = new SalesOrderItemRequest(
                100L, new BigDecimal("3"), new BigDecimal("200.00"), new BigDecimal("15.00"));

        SalesOrderItem result = SalesOrderMapper.toItemEntity(request, 1L, new BigDecimal("10.00"));

        assertEquals(new BigDecimal("200.00"), result.getBaseUnitPrice());
        assertEquals(new BigDecimal("230.00"), result.getUnitPrice()); // 200 * 1.15
        assertEquals(new BigDecimal("15.00"), result.getProfitMargin());
        assertEquals(new BigDecimal("690.00"), result.getTotalPrice()); // 230 * 3
    }

    @Test
    void toItemEntity_margemPropriaZero_usaMargemDoCabecalho() {
        SalesOrderItemRequest request = new SalesOrderItemRequest(
                100L, new BigDecimal("1"), new BigDecimal("50.00"), BigDecimal.ZERO);

        SalesOrderItem result = SalesOrderMapper.toItemEntity(request, 1L, new BigDecimal("20.00"));

        assertEquals(new BigDecimal("60.00"), result.getUnitPrice()); // 50 * 1.20
        assertEquals(BigDecimal.ZERO, result.getProfitMargin());
        assertEquals(new BigDecimal("60.00"), result.getTotalPrice());
    }

    @Test
    void fromQuotation_mapeiaCamposCorretamente() {
        Quotation source = new Quotation();
        source.setId(1L);
        source.setNumber(1500L);
        source.setCustomerId(1L);
        source.setAttention("João");
        source.setSellerId(10L);
        source.setDiscountType(DiscountType.PERCENT);
        source.setDiscount(new BigDecimal("5.00"));
        source.setPaymentCondition(PaymentCondition.PRAZO_30_DIAS);
        source.setNotes("Nota original");
        source.setFreightType(FreightType.CIF);
        source.setFreightValue(new BigDecimal("20.00"));
        source.setCarrierId(5L);

        QuotationItem sourceItem = new QuotationItem();
        sourceItem.setProductId(100L);
        sourceItem.setQuantity(new BigDecimal("2"));
        sourceItem.setUnitPrice(new BigDecimal("110.00"));
        sourceItem.setBaseUnitPrice(new BigDecimal("100.00"));
        sourceItem.setProfitMargin(new BigDecimal("10.00"));
        sourceItem.setTotalPrice(new BigDecimal("220.00"));

        SalesOrderFromQuotationRequest override = new SalesOrderFromQuotationRequest(
                "Novo Contato", PaymentCondition.PARCELAS_30_60_90, "Nova observação");

        SalesOrder result = SalesOrderMapper.fromQuotation(source, List.of(sourceItem), override);

        assertEquals(1L, result.getQuotationId());
        assertEquals(1500L, result.getQuotationNumber());
        assertEquals(1L, result.getCustomerId());
        assertEquals("Novo Contato", result.getAttention()); // override
        assertEquals(10L, result.getSellerId());
        assertEquals(DiscountType.PERCENT, result.getDiscountType());
        assertEquals(PaymentCondition.PARCELAS_30_60_90, result.getPaymentCondition()); // override
        assertEquals("Nova observação", result.getNotes()); // override
        assertEquals(FreightType.CIF, result.getFreightType());
        assertEquals(new BigDecimal("20.00"), result.getFreightValue());
        assertEquals(5L, result.getCarrierId());
    }

    @Test
    void fromQuotation_semOverride_copiaDaProposta() {
        Quotation source = new Quotation();
        source.setId(1L);
        source.setNumber(1500L);
        source.setCustomerId(1L);
        source.setAttention("João");
        source.setSellerId(10L);
        source.setPaymentCondition(PaymentCondition.PRAZO_30_DIAS);
        source.setNotes("Nota original");

        SalesOrder result = SalesOrderMapper.fromQuotation(source, List.of(), null);

        assertEquals("João", result.getAttention());
        assertEquals(PaymentCondition.PRAZO_30_DIAS, result.getPaymentCondition());
        assertEquals("Nota original", result.getNotes());
    }

    @Test
    void fromQuotationItem_copiaPrecosSemReaplicarMargem() {
        QuotationItem source = new QuotationItem();
        source.setProductId(100L);
        source.setQuantity(new BigDecimal("2"));
        source.setUnitPrice(new BigDecimal("110.00"));
        source.setBaseUnitPrice(new BigDecimal("100.00"));
        source.setProfitMargin(new BigDecimal("10.00"));
        source.setTotalPrice(new BigDecimal("220.00"));

        SalesOrderItem result = SalesOrderMapper.fromQuotationItem(source, 1L, new BigDecimal("99.00"));

        assertEquals(1L, result.getSalesOrderId());
        assertEquals(100L, result.getProductId());
        assertEquals(new BigDecimal("2"), result.getQuantity());
        assertEquals(new BigDecimal("110.00"), result.getUnitPrice()); // snapshot preservado
        assertEquals(new BigDecimal("100.00"), result.getBaseUnitPrice());
        assertEquals(new BigDecimal("10.00"), result.getProfitMargin());
        assertEquals(new BigDecimal("220.00"), result.getTotalPrice());
    }

    @Test
    void effectiveMargin_itemMarginNula_usaHeader() {
        BigDecimal result = SalesOrderMapper.effectiveMargin(null, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void effectiveMargin_itemMarginZero_usaHeader() {
        BigDecimal result = SalesOrderMapper.effectiveMargin(BigDecimal.ZERO, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void effectiveMargin_itemMarginPresente_usaItem() {
        BigDecimal result = SalesOrderMapper.effectiveMargin(new BigDecimal("15.00"), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("15.00"), result);
    }

    @Test
    void applyProfitMargin_semMargem_preservaPreco() {
        BigDecimal result = SalesOrderMapper.applyProfitMargin(new BigDecimal("100.00"), null);
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void applyProfitMargin_comMargem_aplicaFator() {
        BigDecimal result = SalesOrderMapper.applyProfitMargin(new BigDecimal("100.00"), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("110.00"), result);
    }

    @Test
    void applyProfitMargin_precoNulo_retornaZero() {
        BigDecimal result = SalesOrderMapper.applyProfitMargin(null, new BigDecimal("10.00"));
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateItemTotalPrice_valoresNormais_calculaCorretamente() {
        BigDecimal result = SalesOrderMapper.calculateItemTotalPrice(
                new BigDecimal("100.00"), new BigDecimal("2"), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("220.00"), result);
    }

    @Test
    void calculateItemTotalPrice_unitPriceNulo_retornaZero() {
        BigDecimal result = SalesOrderMapper.calculateItemTotalPrice(null, BigDecimal.ONE, BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateItemTotalPrice_quantityNulo_retornaZero() {
        BigDecimal result = SalesOrderMapper.calculateItemTotalPrice(BigDecimal.TEN, null, BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        SalesOrder order = new SalesOrder();
        order.setCustomerId(1L);
        order.setAttention("Original");
        order.setSellerId(10L);
        order.setDiscountType(DiscountType.AMOUNT);
        order.setDiscount(new BigDecimal("10.00"));
        order.setPaymentCondition(PaymentCondition.PRAZO_30_DIAS);
        order.setNotes("Nota original");
        order.setFreightType(FreightType.CIF);
        order.setFreightValue(new BigDecimal("20.00"));
        order.setProfitMargin(new BigDecimal("5.00"));
        order.setCarrierId(1L);

        SalesOrderUpdateRequest update = new SalesOrderUpdateRequest(
                2L, null, "Novo", 20L, null,
                DiscountType.PERCENT, new BigDecimal("15.00"),
                PaymentCondition.PARCELAS_30_60_90, "Nova nota",
                FreightType.FOB, new BigDecimal("30.00"), null, new BigDecimal("10.00"));

        SalesOrderMapper.applyUpdate(order, update);

        assertEquals(2L, order.getCustomerId());
        assertEquals("Novo", order.getAttention());
        assertEquals(20L, order.getSellerId());
        assertEquals(DiscountType.PERCENT, order.getDiscountType());
        assertEquals(new BigDecimal("15.00"), order.getDiscount());
        assertEquals(PaymentCondition.PARCELAS_30_60_90, order.getPaymentCondition());
        assertEquals("Nova nota", order.getNotes());
        assertEquals(FreightType.FOB, order.getFreightType());
        assertEquals(new BigDecimal("30.00"), order.getFreightValue());
        assertEquals(new BigDecimal("10.00"), order.getProfitMargin());
        assertNull(order.getCarrierId());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        SalesOrder order = new SalesOrder();
        order.setCustomerId(1L);
        order.setAttention("Original");
        order.setSellerId(10L);
        order.setCarrierId(5L);

        SalesOrderUpdateRequest update = new SalesOrderUpdateRequest(
                null, null, null, null, null,
                null, null, null, null,
                null, null, 5L, null);

        SalesOrderMapper.applyUpdate(order, update);

        assertEquals(1L, order.getCustomerId());
        assertEquals("Original", order.getAttention());
        assertEquals(10L, order.getSellerId());
        assertEquals(5L, order.getCarrierId());
    }
}
