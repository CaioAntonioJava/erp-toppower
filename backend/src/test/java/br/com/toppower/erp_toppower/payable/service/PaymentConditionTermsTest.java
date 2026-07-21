package br.com.toppower.erp_toppower.payable.service;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários de {@link PaymentConditionTerms}.
 *
 * <p>Cobre a extração dos prazos (em dias) de cada parcela a partir do
 * enum {@link PaymentCondition} ou de texto livre, usada para gerar as
 * parcelas programadas de uma conta a pagar.</p>
 */
class PaymentConditionTermsTest {

    @Test
    void terms_avistaConditions_returnSingleZeroTerm() {
        assertEquals(List.of(0), PaymentConditionTerms.terms(PaymentCondition.A_VISTA_DINHEIRO));
        assertEquals(List.of(0), PaymentConditionTerms.terms(PaymentCondition.PIX));
        assertEquals(List.of(0), PaymentConditionTerms.terms(PaymentCondition.BOLETO_A_VISTA));
    }

    @Test
    void terms_nullCondition_returnsDefaultSingleTerm() {
        assertEquals(List.of(PaymentConditionTerms.DEFAULT_DAYS),
                PaymentConditionTerms.terms((PaymentCondition) null));
    }

    @Test
    void terms_singleTermCondition_returnsSingleNumber() {
        assertEquals(List.of(30), PaymentConditionTerms.terms(PaymentCondition.PRAZO_30_DIAS));
        assertEquals(List.of(15), PaymentConditionTerms.terms(PaymentCondition.BOLETO_15_DIAS));
        assertEquals(List.of(45), PaymentConditionTerms.terms(PaymentCondition.FATURADO_45_DIAS));
    }

    @Test
    void terms_parcelasCondition_returnsAllNumbers() {
        assertEquals(List.of(30, 60, 90),
                PaymentConditionTerms.terms(PaymentCondition.PARCELAS_30_60_90));
        assertEquals(List.of(30, 60, 90, 120),
                PaymentConditionTerms.terms(PaymentCondition.PARCELAS_30_60_90_120));
        assertEquals(List.of(30, 60, 90, 120, 150),
                PaymentConditionTerms.terms(PaymentCondition.PARCELAS_30_60_90_120_150));
    }

    @Test
    void terms_textoSimples_extraiNumeros() {
        assertEquals(List.of(30, 60, 90), PaymentConditionTerms.terms("30/60/90"));
        assertEquals(List.of(30), PaymentConditionTerms.terms("30 dias"));
        assertEquals(List.of(45), PaymentConditionTerms.terms("Prazo 45"));
    }

    @Test
    void terms_textoAvista_retornaZero() {
        assertEquals(List.of(0), PaymentConditionTerms.terms("À vista"));
        assertEquals(List.of(0), PaymentConditionTerms.terms("A vista"));
        assertEquals(List.of(0), PaymentConditionTerms.terms("avista"));
    }

    @Test
    void terms_textoVazio_retornaDefault() {
        assertEquals(List.of(PaymentConditionTerms.DEFAULT_DAYS),
                PaymentConditionTerms.terms((String) null));
        assertEquals(List.of(PaymentConditionTerms.DEFAULT_DAYS),
                PaymentConditionTerms.terms(""));
        assertEquals(List.of(PaymentConditionTerms.DEFAULT_DAYS),
                PaymentConditionTerms.terms("   "));
    }

    @Test
    void terms_textoSemNumeros_retornaDefault() {
        // Texto sem nenhum numeral cai no fallback.
        List<Integer> result = PaymentConditionTerms.terms("Entrada");
        assertTrue(result.size() == 1);
    }
}