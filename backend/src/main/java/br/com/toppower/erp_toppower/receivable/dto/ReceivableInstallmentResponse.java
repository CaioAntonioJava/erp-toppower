package br.com.toppower.erp_toppower.receivable.dto;

import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação pública de uma parcela programada de conta a receber,
 * retornada dentro do {@link ReceivableResponse} e no endpoint de
 * listagem de parcelas.
 */
@Schema(name = "ReceivableInstallmentResponse",
        description = "Parcela programada de uma conta a receber.")
public record ReceivableInstallmentResponse(

        @Schema(description = "Identificador único da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Número sequencial da parcela (1..N).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int installmentNumber,

        @Schema(description = "Valor da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Valor já recebido desta parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal paidAmount,

        @Schema(description = "Saldo devedor da parcela (amount - paidAmount).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balance,

        @Schema(description = "Vencimento programado da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Status da parcela.",
                allowableValues = {"ABERTO", "PAGO", "CANCELADO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ReceivableStatus status,

        @Schema(description = "Data do último pagamento registrado para esta parcela.")
        LocalDate paymentDate
) {
}