package br.com.toppower.erp_toppower.receivable.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Representação pública de um pagamento de conta a receber, retornada
 * dentro do {@link ReceivableResponse} na lista de histórico. Inclui o
 * número da parcela a que o pagamento está vinculado (zero quando o
 * pagamento é de uma conta antiga sem parcela).
 */
@Schema(name = "ReceivablePaymentResponse",
        description = "Pagamento avulso de uma conta a receber.")
public record ReceivablePaymentResponse(

        @Schema(description = "Identificador único do pagamento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "ID da parcela vinculada, se aplicável (contas com parcelas).")
        Long installmentId,

        @Schema(description = "Número da parcela vinculada (0 para pagamentos de contas "
                + "antigas sem parcela).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int installmentNumber,

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