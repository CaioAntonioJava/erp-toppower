package br.com.toppower.erp_toppower.receivable.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatório de recebimentos (fluxo) num período: totaliza os pagamentos
 * recebidos por granularidade (dia/semana/mês) e por cliente.
 */
@Schema(name = "ReceivableFlowReportResponse",
        description = "Relatório de recebimentos em um período.")
public record ReceivableFlowReportResponse(

        @Schema(description = "Início do período (inclusive).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate from,

        @Schema(description = "Fim do período (inclusive).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate to,

        @Schema(description = "Granularidade do agrupamento por período.",
                allowableValues = {"DAY", "WEEK", "MONTH"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String granularity,

        @Schema(description = "Total recebido no período.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalReceived,

        @Schema(description = "Quantidade de pagamentos no período.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long paymentCount,

        @Schema(description = "Recebimentos agrupados por período, ordenados asc.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<FlowByPeriod> byPeriod,

        @Schema(description = "Recebimentos agrupados por cliente, ordenado por total desc.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<FlowByClient> byClient
) {

    /**
     * Recebimentos em um período (dia/semana/mês).
     */
    @Schema(name = "FlowByPeriod", description = "Recebimentos agrupados por período.")
    public record FlowByPeriod(

            @Schema(description = "Data de início do período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate periodStart,

            @Schema(description = "Rótulo de exibição do período (ex.: '01/2026', 'Sem 03', '15/01').",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String label,

            @Schema(description = "Total recebido no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal received,

            @Schema(description = "Quantidade de pagamentos no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long paymentCount
    ) {}

    /**
     * Recebimentos de um cliente no período.
     */
    @Schema(name = "FlowByClient", description = "Recebimentos agrupados por cliente.")
    public record FlowByClient(

            @Schema(description = "ID do cliente (customer ou company).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Long clientId,

            @Schema(description = "Nome de exibição do cliente.")
            String clientName,

            @Schema(description = "Total recebido pelo cliente no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal totalReceived,

            @Schema(description = "Quantidade de pagamentos do cliente no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long paymentCount
    ) {}
}