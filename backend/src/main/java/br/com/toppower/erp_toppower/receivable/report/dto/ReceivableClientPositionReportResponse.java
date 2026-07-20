package br.com.toppower.erp_toppower.receivable.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatório de posição por cliente: visão sintética do total a receber,
 * total recebido, contas em aberto e atraso máximo por cliente.
 */
@Schema(name = "ReceivableClientPositionReportResponse",
        description = "Posição consolidada de contas a receber por cliente.")
public record ReceivableClientPositionReportResponse(

        @Schema(description = "Data de referência do relatório (default: hoje).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate referenceDate,

        @Schema(description = "Posição por cliente, ordenada pelo total a receber (desc).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<ClientPosition> clients
) {

    /**
     * Posição consolidada de um cliente.
     */
    @Schema(name = "ClientPosition", description = "Posição consolidada de contas a receber de um cliente.")
    public record ClientPosition(

            @Schema(description = "ID do cliente (customer ou company).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Long clientId,

            @Schema(description = "Nome de exibição do cliente.")
            String clientName,

            @Schema(description = "Código interno do cliente.")
            String clientCode,

            @Schema(description = "Total a receber (soma dos saldos devedores das contas ABERTO).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal totalToReceive,

            @Schema(description = "Total recebido historicamente (soma de paidAmount).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal totalReceived,

            @Schema(description = "Quantidade de contas em aberto.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long openCount,

            @Schema(description = "Quantidade de contas em aberto e em atraso (dueDate < referência).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long overdueCount,

            @Schema(description = "Maior atraso em dias entre as contas em aberto (0 se nenhuma em atraso).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long maxOverdueDays
    ) {}
}