package br.com.toppower.erp_toppower.payable.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload para registrar um pagamento avulso contra uma parcela de
 * conta a pagar. O valor abate do saldo da parcela; a parcela transita
 * para PAGO automaticamente quando o saldo zera, e a conta pai transita
 * para PAGO quando todas as parcelas estão quitadas.
 */
@Schema(name = "PayablePaymentRequest",
        description = "Dados de um pagamento avulso contra uma parcela de conta a pagar.")
public record PayablePaymentRequest(

        @Schema(description = "Valor do pagamento.",
                example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Valor do pagamento é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor do pagamento deve ser maior que zero")
        BigDecimal amount,

        @Schema(description = "Data em que o pagamento foi realizado.",
                example = "2026-07-25", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Data do pagamento é obrigatória")
        @PastOrPresent(message = "Data do pagamento não pode ser futura")
        LocalDate paymentDate,

        @Schema(description = "Observações (forma de pagamento, comprovante, etc.).",
                example = "TED", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 500)
        @Size(max = 500, message = "Observações devem ter no máximo {max} caracteres")
        String notes
) {
}