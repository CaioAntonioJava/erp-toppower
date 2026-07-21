package br.com.toppower.erp_toppower.receivable.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Preview de uma parcela que seria gerada a partir de uma condição de
 * pagamento. Retornado pelo endpoint de preview, antes de persistir.
 */
@Schema(name = "ReceivableInstallmentPreviewResponse",
        description = "Preview de parcela gerada a partir da condição de pagamento.")
public record ReceivableInstallmentPreviewResponse(

        @Schema(description = "Número sequencial da parcela (1..N).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int installmentNumber,

        @Schema(description = "Valor da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Vencimento programado da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate
) {
}