package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de uma parcela/duplicata da NF-e (tag {@code <dup>}).
 */
@Schema(name = "NfeInstallmentData",
        description = "Dados de uma parcela/duplicata da NF-e.")
public record NfeInstallmentData(

        @Schema(description = "Número da duplicata na NF-e.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String number,

        @Schema(description = "Data de vencimento da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Valor da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount
) {
}