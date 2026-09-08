package br.com.toppower.erp_toppower.cashflow.service;

import br.com.toppower.erp_toppower.cashflow.enums.CashFlowEntryType;
import br.com.toppower.erp_toppower.cashflow.service.CashFlowAggregator.FlowEntry;
import br.com.toppower.erp_toppower.cashflow.service.CashFlowAggregator.Granularity;
import br.com.toppower.erp_toppower.cashflow.service.CashFlowAggregator.PeriodRow;
import br.com.toppower.erp_toppower.cashflow.service.CashFlowAggregator.Totals;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes unitários do {@link CashFlowAggregator}.
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>Soma líquida (entradas − saídas)</li>
 *   <li>Totais de entradas/saídas/resultado com contagem</li>
 *   <li>Agrupamento por mês e por ano com saldo acumulado</li>
 *   <li>Valores nulos tratados como ZERO e escala de 2 casas</li>
 * </ul>
 */
class CashFlowAggregatorTest {

    private static FlowEntry entrada(int year, int month, int day, String amount) {
        return new FlowEntry(CashFlowEntryType.ENTRADA, LocalDate.of(year, month, day),
                new BigDecimal(amount));
    }

    private static FlowEntry saida(int year, int month, int day, String amount) {
        return new FlowEntry(CashFlowEntryType.SAIDA, LocalDate.of(year, month, day),
                new BigDecimal(amount));
    }

    @Test
    void net_somaEntradasSubtraiSaidas() {
        BigDecimal net = CashFlowAggregator.net(List.of(
                entrada(2026, 1, 10, "1000.00"),
                saida(2026, 1, 15, "300.00"),
                entrada(2026, 1, 20, "50.00")));
        assertEquals(new BigDecimal("750.00"), net);
    }

    @Test
    void net_valorNulo_tratadoComoZero() {
        BigDecimal net = CashFlowAggregator.net(List.of(
                new FlowEntry(CashFlowEntryType.ENTRADA, LocalDate.of(2026, 1, 10), null),
                saida(2026, 1, 15, "30.00")));
        assertEquals(new BigDecimal("-30.00"), net);
    }

    @Test
    void net_listaVazia_retornaZero() {
        assertEquals(BigDecimal.ZERO, CashFlowAggregator.net(List.of()));
    }

    @Test
    void totals_somaEContaPorTipo() {
        Totals totals = CashFlowAggregator.totals(List.of(
                entrada(2026, 1, 10, "1000.00"),
                saida(2026, 1, 15, "300.00"),
                entrada(2026, 1, 20, "250.50")));
        assertEquals(new BigDecimal("1250.50"), totals.entradas());
        assertEquals(new BigDecimal("300.00"), totals.saidas());
        assertEquals(new BigDecimal("950.50"), totals.resultado());
        assertEquals(2L, totals.entradasCount());
        assertEquals(1L, totals.saidasCount());
    }

    @Test
    void groupByPeriod_mensal_saldoAcumuladoCorreto() {
        // Saldo base 1.000,00 (saldo anterior ao início da série).
        List<PeriodRow> rows = CashFlowAggregator.groupByPeriod(
                List.of(
                        entrada(2026, 1, 10, "500.00"),
                        saida(2026, 1, 15, "200.00"),
                        entrada(2026, 2, 5, "1000.00"),
                        saida(2026, 3, 9, "700.00")),
                Granularity.MONTH,
                new BigDecimal("1000.00"));

        assertEquals(3, rows.size());

        // Janeiro: +500 -200 (saldo acumulado 1.300)
        assertEquals(LocalDate.of(2026, 1, 1), rows.get(0).periodStart());
        assertEquals("01/2026", rows.get(0).label());
        assertEquals(new BigDecimal("500.00"), rows.get(0).entradas());
        assertEquals(new BigDecimal("200.00"), rows.get(0).saidas());
        assertEquals(new BigDecimal("300.00"), rows.get(0).resultado());
        assertEquals(2L, rows.get(0).count());
        assertEquals(new BigDecimal("1300.00"), rows.get(0).saldoAcumulado());

        // Fevereiro: +1.000 (saldo acumulado 2.300)
        assertEquals("02/2026", rows.get(1).label());
        assertEquals(new BigDecimal("2300.00"), rows.get(1).saldoAcumulado());

        // Março: -700 (saldo acumulado 1.600)
        assertEquals("03/2026", rows.get(2).label());
        assertEquals(new BigDecimal("1600.00"), rows.get(2).saldoAcumulado());
    }

    @Test
    void groupByPeriod_anual_agrupaMesmoAnoEmUmaLinha() {
        List<PeriodRow> rows = CashFlowAggregator.groupByPeriod(
                List.of(
                        entrada(2025, 12, 10, "100.00"),
                        saida(2026, 1, 15, "40.00"),
                        entrada(2026, 6, 5, "900.00")),
                Granularity.YEAR,
                BigDecimal.ZERO);

        assertEquals(2, rows.size());
        assertEquals("2025", rows.get(0).label());
        assertEquals(LocalDate.of(2025, 1, 1), rows.get(0).periodStart());
        assertEquals(new BigDecimal("100.00"), rows.get(0).saldoAcumulado());
        assertEquals("2026", rows.get(1).label());
        assertEquals(new BigDecimal("960.00"), rows.get(1).saldoAcumulado());
    }

    @Test
    void groupByPeriod_saldoBaseNulo_comecaDoZero() {
        List<PeriodRow> rows = CashFlowAggregator.groupByPeriod(
                List.of(saida(2026, 5, 2, "120.00"), entrada(2026, 5, 3, "50.00")),
                Granularity.MONTH,
                null);
        assertEquals(1, rows.size());
        assertEquals("05/2026", rows.get(0).label());
        assertEquals(new BigDecimal("-70.00"), rows.get(0).saldoAcumulado());
    }

    @Test
    void groupByPeriod_listaVazia_retornaVazio() {
        List<PeriodRow> rows = CashFlowAggregator.groupByPeriod(List.of(), Granularity.MONTH,
                new BigDecimal("100.00"));
        assertEquals(List.of(), rows);
    }
}