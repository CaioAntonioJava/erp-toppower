package br.com.toppower.erp_toppower.payable.dto;

import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Versão enxuta de {@link PayableResponse} para listas paginadas.
 * Não inclui parcelas nem histórico de pagamentos.
 */
@Schema(name = "PayableSummaryResponse",
        description = "Resumo de uma conta a pagar para listas paginadas.")
public record PayableSummaryResponse(

        @Schema(description = "Identificador único (ID) da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição/origem da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Valor total da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal value,

        @Schema(description = "Valor já pago.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal paidAmount,

        @Schema(description = "Saldo devedor (value - paidAmount).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balance,

        @Schema(description = "Data de emissão.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate issueDate,

        @Schema(description = "Vencimento-base (1ª parcela).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Status atual.",
                allowableValues = {"ABERTO", "PAGO", "CANCELADO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        PayableStatus status,

        @Schema(description = "Origem da conta.",
                allowableValues = {"MANUAL", "BOLETO", "PURCHASE_INVOICE"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        PayableSource sourceType,

        @Schema(description = "ID do fornecedor.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long supplierId,

        @Schema(description = "Nome de exibição do fornecedor.")
        String supplierName,

        @Schema(description = "CNPJ do fornecedor.")
        String supplierTaxId,

        @Schema(description = "Quantidade de parcelas programadas.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int installmentsCount,

        @Schema(description = "Data do último pagamento registrado.")
        LocalDate paymentDate
) {
}