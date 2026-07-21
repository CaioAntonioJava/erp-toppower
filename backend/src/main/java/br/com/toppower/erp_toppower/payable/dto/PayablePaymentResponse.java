package br.com.toppower.erp_toppower.payable.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Representação pública de um pagamento de conta a pagar, retornada
 * dentro do {@link PayableResponse} na lista de histórico.
 */
@Schema(name = "PayablePaymentResponse",
        description = "Pagamento avulso de uma conta a pagar.")
public record PayablePaymentResponse(

        @Schema(description = "Identificador único do pagamento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "ID da parcela baixada por este pagamento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long installmentId,

        @Schema(description = "Número da parcela baixada (para exibição).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int installmentNumber,

        @Schema(description = "Valor do pagamento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Data em que o pagamento foi realizado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate paymentDate,

        @Schema(description = "Observações do pagamento.")
        String notes,

        @Schema(description = "Data de criação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt
) {
}