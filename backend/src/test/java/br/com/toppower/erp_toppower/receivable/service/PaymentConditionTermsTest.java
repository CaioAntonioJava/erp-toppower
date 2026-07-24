package br.com.toppower.erp_toppower.receivable.service;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários de {@link br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms}.
 *
 * <p>Cobre a extração dos prazos (em dias) de cada parcela a partir do
 * enum {@link PaymentCondition} ou de texto livre, usada para gerar as
 * parcelas programadas de uma conta a receber.</p>
 */
class PaymentConditionTermsTest {

    @Test
    void terms_avistaConditions_returnSingleZeroTerm() {
        assertEquals(List.of(0), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(PaymentCondition.A_VISTA_DINHEIRO));
        assertEquals(List.of(0), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(PaymentCondition.PIX));
        assertEquals(List.of(0), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(PaymentCondition.BOLETO_A_VISTA));
    }

    @Test
    void terms_nullCondition_returnsDefaultSingleTerm() {
        assertEquals(List.of(br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.DEFAULT_DAYS),
                br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms((PaymentCondition) null));
    }

    @Test
    void terms_singleTermCondition_returnsSingleNumber() {
        assertEquals(List.of(30), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(PaymentCondition.PRAZO_30_DIAS));
        assertEquals(List.of(15), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(PaymentCondition.BOLETO_15_DIAS));
        assertEquals(List.of(45), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(PaymentCondition.FATURADO_45_DIAS));
    }

    @Test
    void terms_parcelasCondition_returnsAllNumbers() {
        assertEquals(List.of(30, 60, 90),
                br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(PaymentCondition.PARCELAS_30_60_90));
        assertEquals(List.of(30, 60, 90, 120),
                br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(PaymentCondition.PARCELAS_30_60_90_120));
        assertEquals(List.of(30, 60, 90, 120, 150),
                br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(PaymentCondition.PARCELAS_30_60_90_120_150));
    }

    @Test
    void terms_textoSimples_extraiNumeros() {
        assertEquals(List.of(30, 60, 90), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms("30/60/90"));
        assertEquals(List.of(30), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms("30 dias"));
        assertEquals(List.of(45), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms("Prazo 45"));
    }

    @Test
    void terms_textoAvista_retornaZero() {
        assertEquals(List.of(0), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms("À vista"));
        assertEquals(List.of(0), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms("A vista"));
        assertEquals(List.of(0), br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms("avista"));
    }

    @Test
    void terms_textoVazio_retornaDefault() {
        assertEquals(List.of(br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.DEFAULT_DAYS),
                br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms((String) null));
        assertEquals(List.of(br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.DEFAULT_DAYS),
                br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms(""));
        assertEquals(List.of(br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.DEFAULT_DAYS),
                br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms("   "));
    }

    @Test
    void terms_textoSemNumeros_retornaDefault() {
        List<Integer> result = br.com.toppower.erp_toppower.receivable.service.PaymentConditionTerms.terms("Entrada");
        assertTrue(result.size() == 1);
    }
}
