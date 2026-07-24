package br.com.toppower.erp_toppower.sales.quotation.mapper;

import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationCreateRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationItemRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationUpdateRequest;
import br.com.toppower.erp_toppower.sales.quotation.entity.Quotation;
import br.com.toppower.erp_toppower.sales.quotation.entity.QuotationItem;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link QuotationMapper}.
 *
 * <p>Cobre toEntity, toItemEntity, applyUpdate, effectiveMargin,
 * applyProfitMargin e calculateItemTotalPrice.</p>
 */
class QuotationMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        QuotationCreateRequest request = new QuotationCreateRequest(
                1L, null, "João", 10L,
                List.of(new QuotationItemRequest(100L, new BigDecimal("2"), new BigDecimal("100.00"), null)),
                DiscountType.PERCENT, new BigDecimal("5.00"), 15,
                PaymentCondition.PRAZO_30_DIAS, "Observação",
                FreightType.CIF, new BigDecimal("20.00"), new BigDecimal("10.00"), 5L);

        Quotation result = QuotationMapper.toEntity(request);

        assertEquals(1L, result.getCustomerId());
        assertNull(result.getCompanyId());
        assertEquals("João", result.getAttention());
        assertEquals(10L, result.getSellerId());
        assertEquals(DiscountType.PERCENT, result.getDiscountType());
        assertEquals(new BigDecimal("5.00"), result.getDiscount());
        assertEquals(15, result.getValidityDays());
        assertEquals(PaymentCondition.PRAZO_30_DIAS, result.getPaymentCondition());
        assertEquals("Observação", result.getNotes());
        assertEquals(FreightType.CIF, result.getFreightType());
        assertEquals(new BigDecimal("20.00"), result.getFreightValue());
        assertEquals(new BigDecimal("10.00"), result.getProfitMargin());
        assertEquals(5L, result.getCarrierId());
    }

    @Test
    void toItemEntity_semMargemPropria_usaMargemDoCabecalho() {
        QuotationItemRequest request = new QuotationItemRequest(
                100L, new BigDecimal("2"), new BigDecimal("100.00"), null);

        QuotationItem result = QuotationMapper.toItemEntity(request, 1L, new BigDecimal("10.00"));

        assertEquals(1L, result.getQuotationId());
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
        QuotationItemRequest request = new QuotationItemRequest(
                100L, new BigDecimal("3"), new BigDecimal("200.00"), new BigDecimal("15.00"));

        QuotationItem result = QuotationMapper.toItemEntity(request, 1L, new BigDecimal("10.00"));

        assertEquals(new BigDecimal("200.00"), result.getBaseUnitPrice());
        // unitPrice = 200 * (1 + 15/100) = 230.00
        assertEquals(new BigDecimal("230.00"), result.getUnitPrice());
        assertEquals(new BigDecimal("15.00"), result.getProfitMargin());
        // totalPrice = 230 * 3 = 690.00
        assertEquals(new BigDecimal("690.00"), result.getTotalPrice());
    }

    @Test
    void toItemEntity_margemPropriaZero_usaMargemDoCabecalho() {
        QuotationItemRequest request = new QuotationItemRequest(
                100L, new BigDecimal("1"), new BigDecimal("50.00"), BigDecimal.ZERO);

        QuotationItem result = QuotationMapper.toItemEntity(request, 1L, new BigDecimal("20.00"));

        // effectiveMargin: itemMargin.signum() == 0, então usa headerMargin (20%)
        assertEquals(new BigDecimal("50.00"), result.getBaseUnitPrice());
        assertEquals(new BigDecimal("60.00"), result.getUnitPrice()); // 50 * 1.20
        assertEquals(BigDecimal.ZERO, result.getProfitMargin());
        assertEquals(new BigDecimal("60.00"), result.getTotalPrice());
    }

    @Test
    void toItemEntity_unitPriceNulo_retornaZero() {
        QuotationItemRequest request = new QuotationItemRequest(
                100L, new BigDecimal("2"), null, null);

        QuotationItem result = QuotationMapper.toItemEntity(request, 1L, new BigDecimal("10.00"));

        assertEquals(BigDecimal.ZERO, result.getUnitPrice());
        assertEquals(BigDecimal.ZERO, result.getTotalPrice());
    }

    @Test
    void effectiveMargin_itemMarginNula_usaHeader() {
        BigDecimal result = QuotationMapper.effectiveMargin(null, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void effectiveMargin_itemMarginZero_usaHeader() {
        BigDecimal result = QuotationMapper.effectiveMargin(BigDecimal.ZERO, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void effectiveMargin_itemMarginPresente_usaItem() {
        BigDecimal result = QuotationMapper.effectiveMargin(new BigDecimal("15.00"), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("15.00"), result);
    }

    @Test
    void applyProfitMargin_semMargem_preservaPreco() {
        BigDecimal result = QuotationMapper.applyProfitMargin(new BigDecimal("100.00"), null);
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void applyProfitMargin_comMargem_aplicaFator() {
        BigDecimal result = QuotationMapper.applyProfitMargin(new BigDecimal("100.00"), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("110.00"), result);
    }

    @Test
    void applyProfitMargin_precoNulo_retornaZero() {
        BigDecimal result = QuotationMapper.applyProfitMargin(null, new BigDecimal("10.00"));
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateItemTotalPrice_valoresNormais_calculaCorretamente() {
        BigDecimal result = QuotationMapper.calculateItemTotalPrice(
                new BigDecimal("100.00"), new BigDecimal("2"), new BigDecimal("10.00"));
        // 100 * 1.10 * 2 = 220.00
        assertEquals(new BigDecimal("220.00"), result);
    }

    @Test
    void calculateItemTotalPrice_unitPriceNulo_retornaZero() {
        BigDecimal result = QuotationMapper.calculateItemTotalPrice(null, BigDecimal.ONE, BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateItemTotalPrice_quantityNulo_retornaZero() {
        BigDecimal result = QuotationMapper.calculateItemTotalPrice(BigDecimal.TEN, null, BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Quotation quotation = new Quotation();
        quotation.setCustomerId(1L);
        quotation.setAttention("Original");
        quotation.setSellerId(10L);
        quotation.setDiscountType(DiscountType.AMOUNT);
        quotation.setDiscount(new BigDecimal("10.00"));
        quotation.setValidityDays(15);
        quotation.setPaymentCondition(PaymentCondition.PRAZO_30_DIAS);
        quotation.setNotes("Nota original");
        quotation.setFreightType(FreightType.CIF);
        quotation.setFreightValue(new BigDecimal("20.00"));
        quotation.setProfitMargin(new BigDecimal("5.00"));
        quotation.setCarrierId(1L);

        QuotationUpdateRequest update = new QuotationUpdateRequest(
                2L, null, "Novo", 20L, null,
                DiscountType.PERCENT, new BigDecimal("15.00"), 30,
                PaymentCondition.PARCELAS_30_60_90, "Nova nota",
                FreightType.FOB, new BigDecimal("30.00"), new BigDecimal("10.00"), null);

        QuotationMapper.applyUpdate(quotation, update);

        assertEquals(2L, quotation.getCustomerId());
        assertEquals("Novo", quotation.getAttention());
        assertEquals(20L, quotation.getSellerId());
        assertEquals(DiscountType.PERCENT, quotation.getDiscountType());
        assertEquals(new BigDecimal("15.00"), quotation.getDiscount());
        assertEquals(30, quotation.getValidityDays());
        assertEquals(PaymentCondition.PARCELAS_30_60_90, quotation.getPaymentCondition());
        assertEquals("Nova nota", quotation.getNotes());
        assertEquals(FreightType.FOB, quotation.getFreightType());
        assertEquals(new BigDecimal("30.00"), quotation.getFreightValue());
        assertEquals(new BigDecimal("10.00"), quotation.getProfitMargin());
        assertNull(quotation.getCarrierId()); // carrierId foi enviado como null
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Quotation quotation = new Quotation();
        quotation.setCustomerId(1L);
        quotation.setAttention("Original");
        quotation.setSellerId(10L);
        quotation.setCarrierId(5L);

        QuotationUpdateRequest update = new QuotationUpdateRequest(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, 5L); // carrierId = 5L (não altera)

        QuotationMapper.applyUpdate(quotation, update);

        assertEquals(1L, quotation.getCustomerId());
        assertEquals("Original", quotation.getAttention());
        assertEquals(10L, quotation.getSellerId());
        assertEquals(5L, quotation.getCarrierId());
    }
}
