package br.com.toppower.erp_toppower.payable.mapper;

import br.com.toppower.erp_toppower.payable.dto.PayableCreateRequest;
import br.com.toppower.erp_toppower.payable.dto.PayableInstallmentRequest;
import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.entity.PayableInstallment;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários de {@link PayableMapper}.
 *
 * <p>Foca na geração de parcelas programadas a partir do request de
 * criação: caso padrão (uma parcela à vista), parcelas explícitas
 * informadas pelo usuário, e validação indireta da coerência dos
 * números de parcela.</p>
 */
class PayableMapperTest {

    @Test
    void toInstallments_semParcelasExplicitas_geraUnicaParcelaAVista() {
        PayableCreateRequest request = new PayableCreateRequest(
                "CONTA AVULSA",
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 7, 21),
                LocalDate.of(2026, 8, 21),
                12L,
                null,
                null);
        Payable payable = new Payable();
        payable.setId(99L);
        payable.setValue(new BigDecimal("1000.00"));
        payable.setIssueDate(LocalDate.of(2026, 7, 21));
        payable.setDueDate(LocalDate.of(2026, 8, 21));

        List<PayableInstallment> result = PayableMapper.toInstallments(99L, request);

        assertEquals(1, result.size());
        PayableInstallment single = result.get(0);
        assertEquals(99L, single.getPayableId());
        assertEquals(1, single.getInstallmentNumber());
        assertEquals(new BigDecimal("1000.00"), single.getAmount());
        assertEquals(LocalDate.of(2026, 8, 21), single.getDueDate());
        assertEquals(BigDecimal.ZERO, single.getPaidAmount());
        assertEquals(PayableStatus.ABERTO, single.getStatus());
    }

    @Test
    void toInstallments_comParcelasExplicitas_preservaValoresEVencimentos() {
        PayableCreateRequest request = new PayableCreateRequest(
                "BOLETO PARCELADO",
                new BigDecimal("1500.00"),
                LocalDate.of(2026, 7, 21),
                LocalDate.of(2026, 8, 21),
                12L,
                null,
                List.of(
                        new PayableInstallmentRequest(new BigDecimal("500.00"),
                                LocalDate.of(2026, 8, 21)),
                        new PayableInstallmentRequest(new BigDecimal("500.00"),
                                LocalDate.of(2026, 9, 21)),
                        new PayableInstallmentRequest(new BigDecimal("500.00"),
                                LocalDate.of(2026, 10, 21))));
        Payable payable = new Payable();
        payable.setId(7L);
        payable.setValue(new BigDecimal("1500.00"));

        List<PayableInstallment> result = PayableMapper.toInstallments(7L, request);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getInstallmentNumber());
        assertEquals(2, result.get(1).getInstallmentNumber());
        assertEquals(3, result.get(2).getInstallmentNumber());
        assertEquals(new BigDecimal("500.00"), result.get(0).getAmount());
        assertEquals(LocalDate.of(2026, 8, 21), result.get(0).getDueDate());
        assertEquals(LocalDate.of(2026, 9, 21), result.get(1).getDueDate());
        assertEquals(LocalDate.of(2026, 10, 21), result.get(2).getDueDate());
        // Todas com paidAmount = 0 e status ABERTO.
        result.forEach(i -> {
            assertEquals(BigDecimal.ZERO, i.getPaidAmount());
            assertEquals(PayableStatus.ABERTO, i.getStatus());
            assertEquals(7L, i.getPayableId());
        });
    }

    @Test
    void toInstallments_listaVazia_geraUnicaParcelaAVista() {
        // Lista vazia deve ser tratada como "sem parcelas" → 1 à vista.
        PayableCreateRequest request = new PayableCreateRequest(
                "CONTA",
                new BigDecimal("250.00"),
                LocalDate.of(2026, 7, 21),
                LocalDate.of(2026, 8, 21),
                1L,
                null,
                List.of());
        Payable payable = new Payable();
        payable.setId(1L);
        payable.setValue(new BigDecimal("250.00"));

        List<PayableInstallment> result = PayableMapper.toInstallments(1L, request);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("250.00"), result.get(0).getAmount());
    }

    @Test
    void toEntity_manualRequest_origemEhManual() {
        PayableCreateRequest request = new PayableCreateRequest(
                "CONTA MANUAL",
                new BigDecimal("800.00"),
                LocalDate.of(2026, 7, 21),
                LocalDate.of(2026, 8, 21),
                5L,
                null,
                null);

        Payable result = PayableMapper.toEntity(request);

        assertEquals("CONTA MANUAL", result.getDescription());
        assertEquals(new BigDecimal("800.00"), result.getValue());
        assertEquals(LocalDate.of(2026, 7, 21), result.getIssueDate());
        assertEquals(LocalDate.of(2026, 8, 21), result.getDueDate());
        assertEquals(5L, result.getSupplierId());
        assertNotNull(result.getSourceType());
        // Origem sempre MANUAL no endpoint de criação manual.
        assertEquals(br.com.toppower.erp_toppower.payable.enums.PayableSource.MANUAL,
                result.getSourceType());
        // paidAmount e status são definidos pelo @PrePersist; não testamos
        // aqui pois não há persistência.
    }

    @Test
    void toInstallmentResponse_calculaSaldoDevedorDaParcela() {
        PayableInstallment inst = new PayableInstallment();
        inst.setId(42L);
        inst.setInstallmentNumber(2);
        inst.setAmount(new BigDecimal("500.00"));
        inst.setPaidAmount(new BigDecimal("150.00"));
        inst.setDueDate(LocalDate.of(2026, 9, 21));
        inst.setStatus(PayableStatus.ABERTO);
        inst.setPaymentDate(LocalDate.of(2026, 8, 15));

        var result = PayableMapper.toInstallmentResponse(inst);

        assertEquals(42L, result.id());
        assertEquals(2, result.installmentNumber());
        assertEquals(new BigDecimal("500.00"), result.amount());
        assertEquals(new BigDecimal("150.00"), result.paidAmount());
        assertEquals(new BigDecimal("350.00"), result.balance());
        assertEquals(LocalDate.of(2026, 9, 21), result.dueDate());
        assertEquals(PayableStatus.ABERTO, result.status());
    }

    @Test
    void toInstallmentResponse_paidAmountNulo_trataComoZero() {
        PayableInstallment inst = new PayableInstallment();
        inst.setId(1L);
        inst.setInstallmentNumber(1);
        inst.setAmount(new BigDecimal("100.00"));
        inst.setPaidAmount(null); // não deve ocorrer, mas o mapper tolera.
        inst.setDueDate(LocalDate.of(2026, 8, 21));
        inst.setStatus(PayableStatus.ABERTO);

        var result = PayableMapper.toInstallmentResponse(inst);

        assertEquals(new BigDecimal("100.00"), result.balance());
        assertTrue(result.paidAmount().compareTo(BigDecimal.ZERO) == 0);
    }
}