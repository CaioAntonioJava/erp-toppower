package br.com.toppower.erp_toppower.cashflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Configuração do fluxo de caixa da organização corrente.
 */
@Schema(name = "CashFlowSettingsResponse",
        description = "Configuração do fluxo de caixa (saldo inicial).")
public record CashFlowSettingsResponse(

        @Schema(description = "Saldo inicial do caixa (antes do primeiro movimento registrado).",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "15000.00")
        BigDecimal openingBalance,

        @Schema(description = "Data/hora da última atualização do saldo inicial.")
        java.time.Instant updatedAt,

        @Schema(description = "Usuário que realizou a última atualização.")
        String updatedBy
) {

    /** Resposta default quando a organização ainda não configurou o saldo inicial. */
    public static CashFlowSettingsResponse empty() {
        return new CashFlowSettingsResponse(BigDecimal.ZERO.setScale(2), null, null);
    }
}