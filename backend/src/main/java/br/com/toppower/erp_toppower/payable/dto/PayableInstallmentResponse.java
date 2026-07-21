package br.com.toppower.erp_toppower.payable.dto;

import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação pública de uma parcela de conta a pagar, retornada
 * dentro do {@link PayableResponse} e nos endpoints de parcelas.
 */
@Schema(name = "PayableInstallmentResponse",
        description = "Parcela programada de uma conta a pagar.")
public record PayableInstallmentResponse(

        @Schema(description = "Identificador único da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Número sequencial da parcela (1..N).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int installmentNumber,

        @Schema(description = "Valor da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Valor já pago da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal paidAmount,

        @Schema(description = "Saldo devedor da parcela (amount - paidAmount).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balance,

        @Schema(description = "Vencimento programado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Status da parcela.",
                allowableValues = {"ABERTO", "PAGO", "CANCELADO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        PayableStatus status,

        @Schema(description = "Data do último pagamento da parcela.")
        LocalDate paymentDate
) {
}