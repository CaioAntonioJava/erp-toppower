package br.com.toppower.erp_toppower.payable.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatório de posição por fornecedor: visão sintética do total a
 * pagar, total pago, parcelas em aberto e atraso máximo por fornecedor.
 */
@Schema(name = "PayableSupplierPositionReportResponse",
        description = "Posição consolidada de contas a pagar por fornecedor.")
public record PayableSupplierPositionReportResponse(

        @Schema(description = "Data de referência do relatório (default: hoje).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate referenceDate,

        @Schema(description = "Posição por fornecedor, ordenada pelo total a pagar (desc).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<SupplierPosition> suppliers
) {

    /**
     * Posição consolidada de um fornecedor.
     */
    @Schema(name = "SupplierPosition",
            description = "Posição consolidada de contas a pagar de um fornecedor.")
    public record SupplierPosition(

            @Schema(description = "ID do fornecedor.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Long supplierId,

            @Schema(description = "Nome de exibição do fornecedor.")
            String supplierName,

            @Schema(description = "CNPJ do fornecedor.")
            String supplierTaxId,

            @Schema(description = "Total a pagar (soma dos saldos devedores das parcelas ABERTO).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal totalToPay,

            @Schema(description = "Total pago historicamente (soma de paidAmount).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal totalPaid,

            @Schema(description = "Quantidade de parcelas em aberto.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long openCount,

            @Schema(description = "Quantidade de parcelas em aberto e em atraso (dueDate < referência).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long overdueCount,

            @Schema(description = "Maior atraso em dias entre as parcelas em aberto (0 se nenhuma em atraso).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long maxOverdueDays
    ) {}
}