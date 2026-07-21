package br.com.toppower.erp_toppower.receivable.dto;

import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Versão enxuta de {@link ReceivableResponse} para listas paginadas.
 * Não inclui parcelas nem histórico de pagamentos.
 */
@Schema(name = "ReceivableSummaryResponse",
        description = "Resumo de uma conta a receber para listas paginadas.")
public record ReceivableSummaryResponse(

        @Schema(description = "Identificador único (ID) da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição/origem da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Valor total da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal value,

        @Schema(description = "Valor já recebido.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal paidAmount,

        @Schema(description = "Saldo devedor (value - paidAmount).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balance,

        @Schema(description = "Data de vencimento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Status atual.",
                allowableValues = {"ABERTO", "PAGO", "CANCELADO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ReceivableStatus status,

        @Schema(description = "Origem da conta.",
                allowableValues = {"MANUAL", "SALES_ORDER", "TECHNICAL_PROPOSAL", "CONTRACT"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ReceivableSource sourceType,

        @Schema(description = "Código do documento de origem (ex.: \"CL-001-2026\", "
                + "\"PL-001-2026\" ou número do pedido). Nulo para contas manuais.")
        String sourceCode,

        @Schema(description = "Nome resolvido do cliente/empresa.")
        String clientName,

        @Schema(description = "Código resolvido do cliente/empresa.")
        String clientCode,

        @Schema(description = "Quantidade de parcelas programadas.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int installmentsCount,

        @Schema(description = "Data do último pagamento registrado.")
        LocalDate paymentDate
) {
}