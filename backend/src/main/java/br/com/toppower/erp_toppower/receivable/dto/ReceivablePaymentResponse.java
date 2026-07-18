package br.com.toppower.erp_toppower.receivable.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Representação pública de um pagamento de conta a receber, retornada
 * dentro do {@link ReceivableResponse} na lista de histórico.
 */
@Schema(name = "ReceivablePaymentResponse",
        description = "Pagamento avulso de uma conta a receber.")
public record ReceivablePaymentResponse(

        @Schema(description = "Identificador único do pagamento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Valor do pagamento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Data em que o pagamento foi recebido.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate paymentDate,

        @Schema(description = "Observações do pagamento.")
        String notes,

        @Schema(description = "Data de criação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt
) {
}