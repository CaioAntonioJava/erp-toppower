package br.com.toppower.erp_toppower.receivable.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatório de contas a receber em aberto (aging): totaliza o saldo
 * devedor das contas {@code ABERTO} por faixa de atraso em relação à
 * data de referência e por cliente.
 */
@Schema(name = "ReceivableAgingReportResponse",
        description = "Relatório aging de contas a receber em aberto.")
public record ReceivableAgingReportResponse(

        @Schema(description = "Data de referência do relatório (default: hoje).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate referenceDate,

        @Schema(description = "Soma de todos os saldos devedores em aberto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalOpenBalance,

        @Schema(description = "Quantidade de contas em aberto.",
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

        @Schema(description = "Aging por cliente, ordenado pelo saldo devedor total (desc).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<AgingByClient> byClient
) {

    /**
     * Contagem e saldo devedor de uma faixa de atraso.
     */
    @Schema(name = "AgingBucket", description = "Contagem e saldo devedor de uma faixa de atraso.")
    public record AgingBucket(

            @Schema(description = "Quantidade de contas na faixa.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            long count,

            @Schema(description = "Soma dos saldos devedores na faixa.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal balance
    ) {}

    /**
     * Aging de um cliente específico.
     */
    @Schema(name = "AgingByClient", description = "Aging de contas em aberto de um cliente.")
    public record AgingByClient(

            @Schema(description = "ID do cliente (customer ou company).",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Long clientId,

            @Schema(description = "Nome de exibição do cliente.")
            String clientName,

            @Schema(description = "Código interno do cliente.")
            String clientCode,

            @Schema(description = "Saldo devedor total do cliente.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal totalBalance,

            @Schema(description = "Quantidade de contas em aberto do cliente.",
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