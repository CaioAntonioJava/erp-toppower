package br.com.toppower.erp_toppower.cashflow.service;

import br.com.toppower.erp_toppower.cashflow.enums.CashFlowEntryType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Agregações puras do fluxo de caixa (sem acesso a banco), extraídas do
 * service para serem testáveis unitariamente. Trabalha sobre
 * {@link FlowEntry} — pares (tipo, data, valor) já normalizados.
 */
public final class CashFlowAggregator {

    private CashFlowAggregator() {
    }

    /** Granularidade do agrupamento por período da série do fluxo. */
    public enum Granularity {
        /** Agrupamento por mês (rótulo "MM/yyyy"). */
        MONTH,
        /** Agrupamento por ano (rótulo "yyyy"). */
        YEAR
    }

    /**
     * Movimento de caixa simplificado para agregação: tipo, data e
     * valor (sempre positivo; o sentido é dado pelo tipo).
     */
    public record FlowEntry(CashFlowEntryType type, LocalDate date, BigDecimal amount) {
    }

    /** Totais de entradas/saídas de um conjunto de movimentos. */
    public record Totals(BigDecimal entradas, BigDecimal saidas, BigDecimal resultado,
                         long entradasCount, long saidasCount) {
    }

    /** Linha da série por período, com saldo acumulado ao fim do período. */
    public record PeriodRow(LocalDate periodStart, String label, BigDecimal entradas,
                            BigDecimal saidas, BigDecimal resultado, long count,
                            BigDecimal saldoAcumulado) {
    }

    /**
     * Soma líquida (entradas − saídas) dos movimentos informados.
     */
    public static BigDecimal net(List<FlowEntry> entries) {
        BigDecimal net = BigDecimal.ZERO;
        for (FlowEntry entry : entries) {
            BigDecimal amount = amountOf(entry);
            net = (entry.type() == CashFlowEntryType.ENTRADA)
                    ? net.add(amount)
                    : net.subtract(amount);
        }
        return net;
    }

    /**
     * Totais de entradas/saídas/resultado dos movimentos informados.
     */
    public static Totals totals(List<FlowEntry> entries) {
        BigDecimal entradas = BigDecimal.ZERO;
        BigDecimal saidas = BigDecimal.ZERO;
        long entradasCount = 0L;
        long saidasCount = 0L;
        for (FlowEntry entry : entries) {
            BigDecimal amount = amountOf(entry);
            if (entry.type() == CashFlowEntryType.ENTRADA) {
                entradas = entradas.add(amount);
                entradasCount++;
            } else {
                saidas = saidas.add(amount);
                saidasCount++;
            }
        }
        return new Totals(scale(entradas), scale(saidas), scale(entradas.subtract(saidas)),
                entradasCount, saidasCount);
    }

    /**
     * Agrupa os movimentos por mês ({@code MONTH}) ou ano ({@code YEAR})
     * e acumula o saldo a partir de {@code saldoBase}. Os movimentos
     * devem estar ordenados por data ascendente para que o saldo
     * acumulado fique correto. Períodos sem movimentos não aparecem na
     * série.
     *
     * @param entries   movimentos do período (ordenados por data asc)
     * @param gran      granularidade do agrupamento (default MONTH)
     * @param saldoBase saldo do caixa no início da série (saldo anterior)
     * @return linhas ordenadas por período ascendente
     */
    public static List<PeriodRow> groupByPeriod(List<FlowEntry> entries, Granularity gran,
                                                BigDecimal saldoBase) {
        Granularity effective = (gran != null) ? gran : Granularity.MONTH;
        BigDecimal running = (saldoBase != null) ? saldoBase : BigDecimal.ZERO;
        Map<YearMonthKey, PeriodAcc> byPeriod = new TreeMap<>();
        for (FlowEntry entry : entries) {
            YearMonthKey key = keyOf(entry.date(), effective);
            BigDecimal amount = amountOf(entry);
            boolean entrada = entry.type() == CashFlowEntryType.ENTRADA;
            running = entrada ? running.add(amount) : running.subtract(amount);
            byPeriod.computeIfAbsent(key, k -> new PeriodAcc())
                    .add(entrada, amount, running);
        }
        return byPeriod.entrySet().stream()
                .map(e -> e.getValue().toRow(e.getKey(), effective))
                .toList();
    }

    private static BigDecimal amountOf(FlowEntry entry) {
        return (entry.amount() != null) ? entry.amount() : BigDecimal.ZERO;
    }

    private static YearMonthKey keyOf(LocalDate date, Granularity gran) {
        if (date == null) {
            return new YearMonthKey(0, 1);
        }
        if (gran == Granularity.YEAR) {
            return new YearMonthKey(date.getYear(), 1);
        }
        return new YearMonthKey(date.getYear(), date.getMonthValue());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Chave de bucket por período (mês ou ano reduzido ao mês 1). */
    private record YearMonthKey(int year, int month) implements Comparable<YearMonthKey> {
        @Override
        public int compareTo(YearMonthKey other) {
            int byYear = Integer.compare(year, other.year);
            return (byYear != 0) ? byYear : Integer.compare(month, other.month);
        }
    }

    /** Acumulador de um bucket de período. */
    private static final class PeriodAcc {
        private BigDecimal entradas = BigDecimal.ZERO;
        private BigDecimal saidas = BigDecimal.ZERO;
        private long count = 0L;
        private BigDecimal saldoAcumulado = BigDecimal.ZERO;

        void add(boolean entrada, BigDecimal amount, BigDecimal runningSaldo) {
            if (entrada) {
                entradas = entradas.add(amount);
            } else {
                saidas = saidas.add(amount);
            }
            count++;
            saldoAcumulado = runningSaldo;
        }

        PeriodRow toRow(YearMonthKey key, Granularity gran) {
            String label = (gran == Granularity.YEAR)
                    ? String.valueOf(key.year())
                    : String.format("%02d/%d", key.month(), key.year());
            return new PeriodRow(
                    LocalDate.of(key.year(), key.month(), 1),
                    label,
                    scale(entradas),
                    scale(saidas),
                    scale(entradas.subtract(saidas)),
                    count,
                    scale(saldoAcumulado));
        }
    }
}