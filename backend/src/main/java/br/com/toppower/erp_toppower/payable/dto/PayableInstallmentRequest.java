package br.com.toppower.erp_toppower.payable.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de uma parcela programada informada no cadastro de uma conta
 * a pagar. Quando a conta tem uma única parcela (à vista), o usuário
 * pode omitir a lista de parcelas e informar apenas o vencimento-base
 * — o service cria a parcela 1 automaticamente.
 */
@Schema(name = "PayableInstallmentRequest",
        description = "Dados de uma parcela programada de conta a pagar.")
public record PayableInstallmentRequest(

        @Schema(description = "Valor da parcela.",
                example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Valor da parcela é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor da parcela deve ser maior que zero")
        BigDecimal amount,

        @Schema(description = "Vencimento programado da parcela.",
                example = "2026-08-17", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Data de vencimento da parcela é obrigatória")
        LocalDate dueDate
) {
}