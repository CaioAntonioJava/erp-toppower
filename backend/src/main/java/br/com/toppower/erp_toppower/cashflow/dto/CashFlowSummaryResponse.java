package br.com.toppower.erp_toppower.cashflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumo consolidado do fluxo de caixa em um período: saldo anterior,
 * totais de entradas/saídas, resultado e agregação por mês/ano.
 *
 * <p>Semântica dos saldos: {@code saldoAnterior} e {@code saldoFinal}
 são <b>reais</b> (consideram todos os movimentos, independentemente
 dos filtros aplicados); os totais e a série por período respeitam os
 filtros informados.</p>
 */
@Schema(name = "CashFlowSummaryResponse",
        description = "Resumo consolidado do fluxo de caixa em um período.")
public record CashFlowSummaryResponse(

        @Schema(description = "Início do período (inclusive).")
        LocalDate startDate,

        @Schema(description = "Fim do período (inclusive).")
        LocalDate endDate,

        @Schema(description = "Saldo inicial configurado pela organização.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal openingBalance,

        @Schema(description = "Saldo do caixa no início do período (saldo inicial + movimentos "
                + "anteriores a startDate, sem aplicar filtros).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal saldoAnterior,

        @Schema(description = "Total de entradas no período (respeita os filtros).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalEntradas,

        @Schema(description = "Total de saídas no período (respeita os filtros).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalSaidas,

        @Schema(description = "Resultado do período (entradas − saídas; respeita os filtros).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal resultado,

        @Schema(description = "Quantidade de lançamentos de entrada no período.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long entradasCount,

        @Schema(description = "Quantidade de lançamentos de saída no período.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long saidasCount,

        @Schema(description = "Série por período (mês/ano) com saldo acumulado, ordenada asc.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<PeriodRow> periods
) {

    /**
     * Linha da série por período (mês ou ano), com saldo acumulado do
     * caixa ao fim do período.
     */
    @Schema(name = "CashFlowPeriodRow",
            description = "Agregação do fluxo de caixa em um período (mês ou ano).")
    public record PeriodRow(

            @Schema(description = "Data de início do período (1º dia do mês/ano).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate periodStart,

            @Schema(description = "Rótulo de exibição do período (ex.: '01/2026' ou '2026').",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String label,

            @Schema(description = "Total de entradas no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal entradas,

            @Schema(description = "Total de saídas no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal saidas,

            @Schema(description = "Resultado do período (entradas − saídas).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal resultado,

            @Schema(description = "Quantidade de lançamentos no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long count,

            @Schema(description = "Saldo do caixa ao fim do período (real, acumulado a partir "
                    + "do saldo anterior).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal saldoAcumulado
    ) {}
}