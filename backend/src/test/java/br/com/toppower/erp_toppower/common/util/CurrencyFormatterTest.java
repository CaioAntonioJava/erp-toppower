package br.com.toppower.erp_toppower.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes unitários de {@link CurrencyFormatter}.
 *
 * <p>Cobre os casos esperados pelos templates de PDF (cotação, proposta
 * técnica, pedido de venda) e os edge cases (nulos, zeros, precisão
 * acima de 2 casas).</p>
 */
class CurrencyFormatterTest {

    @Test
    void formatBRL_conventionalValue_returnsBRLWithCommaDecimal() {
        // O JDK usa "R$" como prefixo para o locale pt-BR; toleramos o
        // espaço não-quebrável (\u00A0) entre "R$" e o número.
        String result = CurrencyFormatter.formatBRL(new BigDecimal("1234.56"));
        assertEquals("R$\u00A01.234,56", result);
    }

    @Test
    void formatBRL_null_returnsEmDash() {
        assertEquals("—", CurrencyFormatter.formatBRL(null));
    }

    @Test
    void formatBRL_zero_returnsZeroBRL() {
        String result = CurrencyFormatter.formatBRL(BigDecimal.ZERO);
        assertEquals("R$\u00A00,00", result);
    }

    @Test
    void formatBRL_moreThanTwoDecimals_roundsHalfUp() {
        String result = CurrencyFormatter.formatBRL(new BigDecimal("10.555"));
        assertEquals("R$\u00A010,56", result);
    }

    @Test
    void formatPercent_twentyFive_returnsTwentyFivePercent() {
        String result = CurrencyFormatter.formatPercent(new BigDecimal("25"));
        assertEquals("25,00%", result);
    }

    @Test
    void formatPercent_null_returnsZeroPercent() {
        assertEquals("0,00%", CurrencyFormatter.formatPercent(null));
    }

    @Test
    void formatFractionAsPercent_zeroPointOne_returnsTenPercent() {
        String result = CurrencyFormatter.formatFractionAsPercent(new BigDecimal("0.1"));
        assertEquals("10,00%", result);
    }

    @Test
    void formatDate_isoDate_returnsBrazilianFormat() {
        String result = CurrencyFormatter.formatDate(LocalDate.of(2026, 7, 8));
        assertEquals("08/07/2026", result);
    }

    @Test
    void formatDate_null_returnsEmDash() {
        assertEquals("—", CurrencyFormatter.formatDate(null));
    }
}