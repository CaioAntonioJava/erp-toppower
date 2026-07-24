package br.com.toppower.erp_toppower.sales.technicalproposal.mapper;

import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.*;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposal;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalCondition;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalProductItem;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalServiceItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link TechnicalProposalMapper}.
 *
 * <p>Cobre toEntity, toServiceItemEntity, toProductItemEntity, toConditionEntity,
 * applyUpdate, toAddress e calculateProductItemTotalPrice.</p>
 */
class TechnicalProposalMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        TechnicalProposalAddressRequest addressReq = new TechnicalProposalAddressRequest(
                "Rua Exemplo", "100", "Sala 1", "Centro", "São Paulo", "SP", "01310-100");

        TechnicalProposalCreateRequest request = new TechnicalProposalCreateRequest(
                1L, null, addressReq, "Descrição técnica", 2,
                "Eng. João", "joao@email.com", "11999999999",
                List.of(new TechnicalProposalServiceItemRequest("Serviço A", new BigDecimal("500.00"), "CAT_A", 10L)),
                List.of(new TechnicalProposalProductItemRequest(100L, new BigDecimal("2"), new BigDecimal("150.00"))),
                List.of(new TechnicalProposalConditionRequest("Título", "Conteúdo")),
                DiscountType.PERCENT, new BigDecimal("10.00"), new BigDecimal("50.00"),
                "3 dias", PaymentCondition.PRAZO_30_DIAS, "10 dias",
                FreightType.CIF, "Observações", 5L, new BigDecimal("2000.00"));

        TechnicalProposal result = TechnicalProposalMapper.toEntity(request);

        assertEquals(1L, result.getCustomerId());
        assertNull(result.getCompanyId());
        assertNotNull(result.getAddress());
        assertEquals("Rua Exemplo", result.getAddress().getStreet());
        assertEquals("Descrição técnica", result.getDescription());
        assertEquals(2, result.getRevision());
        assertEquals("Eng. João", result.getTechnicalResponsible());
        assertEquals("joao@email.com", result.getEmail());
        assertEquals("11999999999", result.getPhone());
        assertEquals(DiscountType.PERCENT, result.getDiscountType());
        assertEquals(new BigDecimal("10.00"), result.getDiscount());
        assertEquals(new BigDecimal("50.00"), result.getFreightValue());
        assertEquals("3 dias", result.getDeliveryDeadline());
        assertEquals(PaymentCondition.PRAZO_30_DIAS, result.getPaymentCondition());
        assertEquals("10 dias", result.getValidity());
        assertEquals(FreightType.CIF, result.getDeliveryType());
        assertEquals("Observações", result.getNotes());
        assertEquals(5L, result.getCarrierId());
        assertEquals(new BigDecimal("2000.00"), result.getGeneralPrice());
    }

    @Test
    void toServiceItemEntity_mapeiaCamposCorretamente() {
        TechnicalProposalServiceItemRequest request = new TechnicalProposalServiceItemRequest(
                "Instalação elétrica", new BigDecimal("1500.00"), "EXECUCAO", 10L);

        TechnicalProposalServiceItem result = TechnicalProposalMapper.toServiceItemEntity(request, 1L);

        assertEquals(1L, result.getTechnicalProposalId());
        assertEquals("Instalação elétrica", result.getDescription());
        assertEquals(new BigDecimal("1500.00"), result.getPrice());
        assertEquals("EXECUCAO", result.getCategory());
        assertEquals(10L, result.getServiceTemplateId());
    }

    @Test
    void toProductItemEntity_mapeiaCamposCorretamente() {
        TechnicalProposalProductItemRequest request = new TechnicalProposalProductItemRequest(
                100L, new BigDecimal("3"), new BigDecimal("200.00"));

        TechnicalProposalProductItem result = TechnicalProposalMapper.toProductItemEntity(request, 1L);

        assertEquals(1L, result.getTechnicalProposalId());
        assertEquals(100L, result.getProductId());
        assertEquals(new BigDecimal("3"), result.getQuantity());
        assertEquals(new BigDecimal("200.00"), result.getUnitPrice());
        assertEquals(new BigDecimal("600.00"), result.getTotalPrice()); // 200 * 3
    }

    @Test
    void toProductItemEntity_unitPriceNulo_retornaTotalZero() {
        TechnicalProposalProductItemRequest request = new TechnicalProposalProductItemRequest(
                100L, new BigDecimal("2"), null);

        TechnicalProposalProductItem result = TechnicalProposalMapper.toProductItemEntity(request, 1L);

        assertEquals(BigDecimal.ZERO, result.getTotalPrice());
    }

    @Test
    void toProductItemEntity_quantityNulo_retornaTotalZero() {
        TechnicalProposalProductItemRequest request = new TechnicalProposalProductItemRequest(
                100L, new BigDecimal("2"), null);

        TechnicalProposalProductItem result = TechnicalProposalMapper.toProductItemEntity(request, 1L);

        assertEquals(BigDecimal.ZERO, result.getTotalPrice());
    }

    @Test
    void toConditionEntity_mapeiaCamposCorretamente() {
        TechnicalProposalConditionRequest request = new TechnicalProposalConditionRequest(
                "Garantia", "Garantia de 12 meses");

        TechnicalProposalCondition result = TechnicalProposalMapper.toConditionEntity(request, 1L, 1);

        assertEquals(1L, result.getTechnicalProposalId());
        assertEquals("Garantia", result.getTitle());
        assertEquals("Garantia de 12 meses", result.getContent());
        assertEquals(1, result.getSortOrder());
    }

    @Test
    void calculateProductItemTotalPrice_valoresNormais_calculaCorretamente() {
        BigDecimal result = TechnicalProposalMapper.calculateProductItemTotalPrice(
                new BigDecimal("100.00"), new BigDecimal("2"));
        assertEquals(new BigDecimal("200.00"), result);
    }

    @Test
    void calculateProductItemTotalPrice_unitPriceNulo_retornaZero() {
        BigDecimal result = TechnicalProposalMapper.calculateProductItemTotalPrice(null, BigDecimal.ONE);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateProductItemTotalPrice_quantityNulo_retornaZero() {
        BigDecimal result = TechnicalProposalMapper.calculateProductItemTotalPrice(BigDecimal.TEN, null);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void toAddress_requestNulo_retornaNull() {
        assertNull(TechnicalProposalMapper.toAddress(null));
    }

    @Test
    void toAddress_mapeiaCamposCorretamente() {
        TechnicalProposalAddressRequest request = new TechnicalProposalAddressRequest(
                "Rua A", "200", "Apto 5", "Centro", "São Paulo", "SP", "01310-000");

        Address result = TechnicalProposalMapper.toAddress(request);

        assertEquals("Rua A", result.getStreet());
        assertEquals("200", result.getNumber());
        assertEquals("Apto 5", result.getComplement());
        assertEquals("Centro", result.getNeighborhood());
        assertEquals("São Paulo", result.getCity());
        assertEquals("SP", result.getState());
        assertEquals("01310-000", result.getZipCode());
    }

    @Test
    void toAddressResponse_addressNulo_retornaNull() {
        assertNull(TechnicalProposalMapper.toAddressResponse(null));
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        TechnicalProposal tp = new TechnicalProposal();
        tp.setCustomerId(1L);
        tp.setDescription("Original");
        tp.setRevision(1);
        tp.setTechnicalResponsible("Eng. Original");
        tp.setEmail("original@email.com");
        tp.setPhone("11111111111");
        tp.setDiscountType(DiscountType.AMOUNT);
        tp.setDiscount(new BigDecimal("10.00"));
        tp.setFreightValue(new BigDecimal("20.00"));
        tp.setDeliveryDeadline("5 dias");
        tp.setPaymentCondition(PaymentCondition.PRAZO_30_DIAS);
        tp.setValidity("15 dias");
        tp.setDeliveryType(FreightType.CIF);
        tp.setNotes("Nota original");
        tp.setCarrierId(1L);
        tp.setGeneralPrice(new BigDecimal("1000.00"));

        TechnicalProposalAddressRequest newAddress = new TechnicalProposalAddressRequest(
                "Rua Nova", "500", null, "Centro", "Rio de Janeiro", "RJ", "20000-000");

        TechnicalProposalUpdateRequest update = new TechnicalProposalUpdateRequest(
                2L, null, newAddress, "Nova descrição", 3,
                "Eng. Novo", "novo@email.com", "22222222222",
                null, null, null,
                DiscountType.PERCENT, new BigDecimal("15.00"), new BigDecimal("30.00"),
                "10 dias", PaymentCondition.PARCELAS_30_60_90, "30 dias",
                FreightType.FOB, "Nova nota", null, new BigDecimal("2000.00"));

        TechnicalProposalMapper.applyUpdate(tp, update);

        assertEquals(2L, tp.getCustomerId());
        assertEquals("Nova descrição", tp.getDescription());
        assertEquals(3, tp.getRevision());
        assertEquals("Eng. Novo", tp.getTechnicalResponsible());
        assertEquals("novo@email.com", tp.getEmail());
        assertEquals("22222222222", tp.getPhone());
        assertEquals("Rua Nova", tp.getAddress().getStreet());
        assertEquals(DiscountType.PERCENT, tp.getDiscountType());
        assertEquals(new BigDecimal("15.00"), tp.getDiscount());
        assertEquals(new BigDecimal("30.00"), tp.getFreightValue());
        assertEquals("10 dias", tp.getDeliveryDeadline());
        assertEquals(PaymentCondition.PARCELAS_30_60_90, tp.getPaymentCondition());
        assertEquals("30 dias", tp.getValidity());
        assertEquals(FreightType.FOB, tp.getDeliveryType());
        assertEquals("Nova nota", tp.getNotes());
        assertNull(tp.getCarrierId()); // carrierId enviado como null
        assertEquals(new BigDecimal("2000.00"), tp.getGeneralPrice());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        TechnicalProposal tp = new TechnicalProposal();
        tp.setCustomerId(1L);
        tp.setDescription("Original");
        tp.setTechnicalResponsible("Eng. Original");
        tp.setCarrierId(5L);

        TechnicalProposalUpdateRequest update = new TechnicalProposalUpdateRequest(
                null, null, null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, 5L, null);

        TechnicalProposalMapper.applyUpdate(tp, update);

        assertEquals(1L, tp.getCustomerId());
        assertEquals("Original", tp.getDescription());
        assertEquals("Eng. Original", tp.getTechnicalResponsible());
        assertEquals(5L, tp.getCarrierId());
    }

    @Test
    void applyUpdate_technicalResponsibleVazio_limpaCampo() {
        TechnicalProposal tp = new TechnicalProposal();
        tp.setTechnicalResponsible("Eng. Original");

        TechnicalProposalUpdateRequest update = new TechnicalProposalUpdateRequest(
                null, null, null, null, null,
                "", null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null, null);

        TechnicalProposalMapper.applyUpdate(tp, update);

        assertNull(tp.getTechnicalResponsible());
    }
}
