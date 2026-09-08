package br.com.toppower.erp_toppower.cashflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Requisição de atualização do saldo inicial do fluxo de caixa da
 * organização corrente.
 */
@Schema(name = "CashFlowSettingsRequest",
        description = "Atualização do saldo inicial do fluxo de caixa.")
public record CashFlowSettingsRequest(

        @Schema(description = "Saldo inicial do caixa (antes do primeiro movimento registrado). "
                + "Pode ser negativo.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "15000.00")
        @NotNull(message = "O saldo inicial é obrigatório.")
        @Digits(integer = 10, fraction = 2,
                message = "O saldo inicial deve ter até 10 dígitos inteiros e 2 decimais.")
        BigDecimal openingBalance
) {
}