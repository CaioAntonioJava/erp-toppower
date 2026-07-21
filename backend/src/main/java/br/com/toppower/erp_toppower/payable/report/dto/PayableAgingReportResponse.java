package br.com.toppower.erp_toppower.payable.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatório de contas a pagar em aberto (aging): totaliza o saldo
 * devedor das parcelas ABERTO por faixa de atraso em relação à data
 * de referência e por fornecedor.
 *
 * <p>Diferente do contas a receber, o aging é calculado por
 * <b>parcela</b> ({@code PayableInstallment.dueDate}), não pela conta
 * pai — pois uma conta com parcelamento 30/60/90 tem parcelas com
 * vencimentos distintos.</p>
 */
@Schema(name = "PayableAgingReportResponse",
        description = "Relatório aging de contas a pagar em aberto (por parcela).")
public record PayableAgingReportResponse(

        @Schema(description = "Data de referência do relatório (default: hoje).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate referenceDate,

        @Schema(description = "Soma de todos os saldos devedores em aberto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalOpenBalance,

        @Schema(description = "Quantidade de parcelas em aberto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long totalOpenCount,

        @Schema(description = "Faixa 0–30 dias de atraso.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AgingBucket bucket0_30,

        @Schema(description = "Faixa 31–60 dias de atraso.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AgingBucket bucket31_60,

        @Schema(description = "Faixa 61–90 dias de atraso.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AgingBucket bucket61_90,

        @Schema(description = "Faixa acima de 90 dias de atraso.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AgingBucket bucket90Plus,

        @Schema(description = "Aging por fornecedor, ordenado pelo saldo devedor total (desc).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<AgingBySupplier> bySupplier
) {

    /**
     * Contagem e saldo devedor de uma faixa de atraso.
     */
    @Schema(name = "AgingBucket", description = "Contagem e saldo devedor de uma faixa de atraso.")
    public record AgingBucket(

            @Schema(description = "Quantidade de parcelas na faixa.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long count,

            @Schema(description = "Soma dos saldos devedores na faixa.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal balance
    ) {}

    /**
     * Aging de um fornecedor específico.
     */
    @Schema(name = "AgingBySupplier", description = "Aging de parcelas em aberto de um fornecedor.")
    public record AgingBySupplier(

            @Schema(description = "ID do fornecedor.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Long supplierId,

            @Schema(description = "Nome de exibição do fornecedor.")
            String supplierName,

            @Schema(description = "CNPJ do fornecedor.")
            String supplierTaxId,

            @Schema(description = "Saldo devedor total do fornecedor.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal totalBalance,

            @Schema(description = "Quantidade de parcelas em aberto do fornecedor.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long count,

            @Schema(description = "Faixa 0–30 dias.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            AgingBucket bucket0_30,

            @Schema(description = "Faixa 31–60 dias.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            AgingBucket bucket31_60,

            @Schema(description = "Faixa 61–90 dias.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            AgingBucket bucket61_90,

            @Schema(description = "Faixa acima de 90 dias.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            AgingBucket bucket90Plus
    ) {}
}