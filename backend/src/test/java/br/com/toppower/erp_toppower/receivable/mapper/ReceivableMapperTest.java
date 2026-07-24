package br.com.toppower.erp_toppower.receivable.mapper;

import br.com.toppower.erp_toppower.receivable.dto.ReceivableCreateRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableInstallmentRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivablePaymentRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableUpdateRequest;
import br.com.toppower.erp_toppower.receivable.entity.Receivable;
import br.com.toppower.erp_toppower.receivable.entity.ReceivableInstallment;
import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link ReceivableMapper}.
 *
 * <p>Cobre toEntity, toInstallments, applyUpdate, toPaymentEntity,
 * toResponse, toSummary, toInstallmentResponse e toPaymentResponse.</p>
 */
class ReceivableMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        ReceivableCreateRequest request = new ReceivableCreateRequest(
                "Serviço de Manutenção", new BigDecimal("3000.00"),
                LocalDate.of(2026, 7, 21), LocalDate.of(2026, 8, 21),
                1L, null, PaymentCondition.PARCELAS_30_60_90, null);

        Receivable result = ReceivableMapper.toEntity(request);

        assertEquals("Serviço de Manutenção", result.getDescription());
        assertEquals(new BigDecimal("3000.00"), result.getValue());
        assertEquals(LocalDate.of(2026, 8, 21), result.getDueDate());
        assertEquals(1L, result.getCustomerId());
        assertNull(result.getCompanyId());
        assertEquals(PaymentCondition.PARCELAS_30_60_90, result.getPaymentCondition());
        assertEquals(ReceivableSource.MANUAL, result.getSourceType());
    }

    @Test
    void toInstallments_semParcelasExplicitas_geraUnicaParcelaAVista() {
        ReceivableCreateRequest request = new ReceivableCreateRequest(
                "Teste", new BigDecimal("1000.00"),
                LocalDate.of(2026, 7, 21), LocalDate.of(2026, 8, 21),
                1L, null, null, null);

        List<ReceivableInstallment> result = ReceivableMapper.toInstallments(99L, request);

        assertEquals(1, result.size());
        ReceivableInstallment single = result.get(0);
        assertEquals(99L, single.getReceivableId());
        assertEquals(1, single.getInstallmentNumber());
        assertEquals(new BigDecimal("1000.00"), single.getAmount());
        assertEquals(LocalDate.of(2026, 8, 21), single.getDueDate());
        assertEquals(BigDecimal.ZERO, single.getPaidAmount());
        assertEquals(ReceivableStatus.ABERTO, single.getStatus());
    }

    @Test
    void toInstallments_listaVazia_geraUnicaParcela() {
        ReceivableCreateRequest request = new ReceivableCreateRequest(
                "Teste", new BigDecimal("500.00"),
                LocalDate.of(2026, 7, 21), LocalDate.of(2026, 8, 21),
                null, 1L, null, List.of());

        List<ReceivableInstallment> result = ReceivableMapper.toInstallments(1L, request);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("500.00"), result.get(0).getAmount());
    }

    @Test
    void toInstallments_comParcelasExplicitas_preservaValores() {
        ReceivableCreateRequest request = new ReceivableCreateRequest(
                "Parcelado", new BigDecimal("1500.00"),
                LocalDate.of(2026, 7, 21), LocalDate.of(2026, 8, 21),
                1L, null, null,
                List.of(
                        new ReceivableInstallmentRequest(new BigDecimal("500.00"), LocalDate.of(2026, 8, 21)),
                        new ReceivableInstallmentRequest(new BigDecimal("500.00"), LocalDate.of(2026, 9, 21)),
                        new ReceivableInstallmentRequest(new BigDecimal("500.00"), LocalDate.of(2026, 10, 21))));

        List<ReceivableInstallment> result = ReceivableMapper.toInstallments(7L, request);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getInstallmentNumber());
        assertEquals(2, result.get(1).getInstallmentNumber());
        assertEquals(3, result.get(2).getInstallmentNumber());
        assertEquals(new BigDecimal("500.00"), result.get(0).getAmount());
        assertEquals(LocalDate.of(2026, 8, 21), result.get(0).getDueDate());
        assertEquals(LocalDate.of(2026, 9, 21), result.get(1).getDueDate());
        assertEquals(LocalDate.of(2026, 10, 21), result.get(2).getDueDate());
        result.forEach(i -> {
            assertEquals(BigDecimal.ZERO, i.getPaidAmount());
            assertEquals(ReceivableStatus.ABERTO, i.getStatus());
            assertEquals(7L, i.getReceivableId());
        });
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Receivable r = new Receivable();
        r.setDescription("Original");
        r.setDueDate(LocalDate.of(2026, 1, 1));
        r.setPaymentCondition(PaymentCondition.A_VISTA_DINHEIRO);

        ReceivableUpdateRequest update = new ReceivableUpdateRequest(
                "Nova descrição", LocalDate.of(2026, 12, 31), PaymentCondition.PARCELAS_30_60_90);

        ReceivableMapper.applyUpdate(r, update);

        assertEquals("Nova descrição", r.getDescription());
        assertEquals(LocalDate.of(2026, 12, 31), r.getDueDate());
        assertEquals(PaymentCondition.PARCELAS_30_60_90, r.getPaymentCondition());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Receivable r = new Receivable();
        r.setDescription("Original");
        r.setDueDate(LocalDate.of(2026, 1, 1));

        ReceivableUpdateRequest update = new ReceivableUpdateRequest(null, null, null);

        ReceivableMapper.applyUpdate(r, update);

        assertEquals("Original", r.getDescription());
        assertEquals(LocalDate.of(2026, 1, 1), r.getDueDate());
    }

    @Test
    void toPaymentEntity_mapeiaCamposCorretamente() {
        ReceivablePaymentRequest request = new ReceivablePaymentRequest(
                new BigDecimal("500.00"), LocalDate.of(2026, 8, 15), "PIX");

        ReceivablePayment result = ReceivableMapper.toPaymentEntity(1L, 10L, request);

        assertEquals(1L, result.getReceivableId());
        assertEquals(10L, result.getInstallmentId());
        assertEquals(new BigDecimal("500.00"), result.getAmount());
        assertEquals(LocalDate.of(2026, 8, 15), result.getPaymentDate());
        assertEquals("PIX", result.getNotes());
    }

    @Test
    void toInstallmentResponse_calculaSaldoDevedor() {
        ReceivableInstallment inst = new ReceivableInstallment();
        inst.setId(42L);
        inst.setInstallmentNumber(2);
        inst.setAmount(new BigDecimal("500.00"));
        inst.setPaidAmount(new BigDecimal("150.00"));
        inst.setDueDate(LocalDate.of(2026, 9, 21));
        inst.setStatus(ReceivableStatus.ABERTO);
        inst.setPaymentDate(LocalDate.of(2026, 8, 15));

        var result = ReceivableMapper.toInstallmentResponse(inst);

        assertEquals(42L, result.id());
        assertEquals(2, result.installmentNumber());
        assertEquals(new BigDecimal("500.00"), result.amount());
        assertEquals(new BigDecimal("150.00"), result.paidAmount());
        assertEquals(new BigDecimal("350.00"), result.balance());
        assertEquals(LocalDate.of(2026, 9, 21), result.dueDate());
        assertEquals(ReceivableStatus.ABERTO, result.status());
    }

    @Test
    void toInstallmentResponse_paidAmountNulo_trataComoZero() {
        ReceivableInstallment inst = new ReceivableInstallment();
        inst.setId(1L);
        inst.setInstallmentNumber(1);
        inst.setAmount(new BigDecimal("100.00"));
        inst.setPaidAmount(null);
        inst.setDueDate(LocalDate.of(2026, 8, 21));
        inst.setStatus(ReceivableStatus.ABERTO);

        var result = ReceivableMapper.toInstallmentResponse(inst);

        assertEquals(new BigDecimal("100.00"), result.balance());
        assertEquals(BigDecimal.ZERO, result.paidAmount());
    }

    @Test
    void toPaymentResponse_comInstallment_resolveNumero() {
        ReceivablePayment p = new ReceivablePayment();
        p.setId(1L);
        p.setInstallmentId(10L);
        p.setAmount(new BigDecimal("500.00"));
        p.setPaymentDate(LocalDate.of(2026, 8, 15));
        p.setNotes("PIX");

        ReceivableInstallment inst = new ReceivableInstallment();
        inst.setId(10L);
        inst.setInstallmentNumber(3);

        var result = ReceivableMapper.toPaymentResponse(p, Map.of(10L, inst));

        assertEquals(1L, result.id());
        assertEquals(10L, result.installmentId());
        assertEquals(3, result.installmentNumber());
        assertEquals(new BigDecimal("500.00"), result.amount());
    }

    @Test
    void toPaymentResponse_semInstallment_installmentNumberZero() {
        ReceivablePayment p = new ReceivablePayment();
        p.setId(1L);
        p.setInstallmentId(null);
        p.setAmount(new BigDecimal("1000.00"));
        p.setPaymentDate(LocalDate.of(2026, 8, 15));

        var result = ReceivableMapper.toPaymentResponse(p, Map.of());

        assertEquals(0, result.installmentNumber());
    }

    @Test
    void toSummary_mapeiaCamposCorretamente() {
        Receivable r = new Receivable();
        r.setId(1L);
        r.setDescription("Conta Teste");
        r.setValue(new BigDecimal("2000.00"));
        r.setPaidAmount(new BigDecimal("500.00"));
        r.setDueDate(LocalDate.of(2026, 9, 21));
        r.setStatus(ReceivableStatus.ABERTO);
        r.setSourceType(ReceivableSource.MANUAL);
        r.setInstallmentsCount(3);
        r.setPaymentDate(null);

        var result = ReceivableMapper.toSummary(r, "Cliente ABC", "CLI001");

        assertEquals(1L, result.id());
        assertEquals("Conta Teste", result.description());
        assertEquals(new BigDecimal("2000.00"), result.value());
        assertEquals(new BigDecimal("500.00"), result.paidAmount());
        assertEquals(new BigDecimal("1500.00"), result.balance());
        assertEquals(LocalDate.of(2026, 9, 21), result.dueDate());
        assertEquals(ReceivableStatus.ABERTO, result.status());
        assertEquals(ReceivableSource.MANUAL, result.sourceType());
        assertEquals("Cliente ABC", result.clientName());
        assertEquals("CLI001", result.clientCode());
        assertEquals(3, result.installmentsCount());
    }

    @Test
    void toSummary_sourceSALES_ORDER_retornaCodigo() {
        Receivable r = new Receivable();
        r.setId(1L);
        r.setDescription("Pedido");
        r.setValue(BigDecimal.TEN);
        r.setPaidAmount(BigDecimal.ZERO);
        r.setDueDate(LocalDate.now());
        r.setStatus(ReceivableStatus.ABERTO);
        r.setSourceType(ReceivableSource.SALES_ORDER);
        r.setSalesOrderCode("PV-2800-2026");
        r.setInstallmentsCount(1);

        var result = ReceivableMapper.toSummary(r, "Cliente", "CLI001");

        assertEquals("PV-2800-2026", result.sourceCode());
    }

    @Test
    void toSummary_sourceMANUAL_sourceCodeNulo() {
        Receivable r = new Receivable();
        r.setId(1L);
        r.setDescription("Manual");
        r.setValue(BigDecimal.TEN);
        r.setPaidAmount(BigDecimal.ZERO);
        r.setDueDate(LocalDate.now());
        r.setStatus(ReceivableStatus.ABERTO);
        r.setSourceType(ReceivableSource.MANUAL);
        r.setInstallmentsCount(1);

        var result = ReceivableMapper.toSummary(r, "Cliente", "CLI001");

        assertNull(result.sourceCode());
    }
}
