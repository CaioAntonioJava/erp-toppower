package br.com.toppower.erp_toppower.payable.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatório de pagamentos (fluxo) num período: totaliza os pagamentos
 * realizados por granularidade (dia/semana/mês) e por fornecedor.
 */
@Schema(name = "PayableFlowReportResponse",
        description = "Relatório de pagamentos realizados em um período.")
public record PayableFlowReportResponse(

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

        @Schema(description = "Total pago no período.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalPaid,

        @Schema(description = "Quantidade de pagamentos no período.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long paymentCount,

        @Schema(description = "Pagamentos agrupados por período, ordenados asc.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<FlowByPeriod> byPeriod,

        @Schema(description = "Pagamentos agrupados por fornecedor, ordenado por total desc.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<FlowBySupplier> bySupplier
) {

    /**
     * Pagamentos em um período (dia/semana/mês).
     */
    @Schema(name = "FlowByPeriod", description = "Pagamentos agrupados por período.")
    public record FlowByPeriod(

            @Schema(description = "Data de início do período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate periodStart,

            @Schema(description = "Rótulo de exibição do período (ex.: '01/2026', 'Sem 03', '15/01').",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String label,

            @Schema(description = "Total pago no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal paid,

            @Schema(description = "Quantidade de pagamentos no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long paymentCount
    ) {}

    /**
     * Pagamentos de um fornecedor no período.
     */
    @Schema(name = "FlowBySupplier", description = "Pagamentos agrupados por fornecedor.")
    public record FlowBySupplier(

            @Schema(description = "ID do fornecedor.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Long supplierId,

            @Schema(description = "Nome de exibição do fornecedor.")
            String supplierName,

            @Schema(description = "Total pago ao fornecedor no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal totalPaid,

            @Schema(description = "Quantidade de pagamentos do fornecedor no período.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long paymentCount
    ) {}
}