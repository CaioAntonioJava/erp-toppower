package br.com.toppower.erp_toppower.receivable.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Atualização parcial (PATCH) de uma conta a receber manual. Apenas
 * campos editáveis são expostos — valor, origem e vínculos com
 * documentos de origem não são alteráveis. Bloqueada para contas PAGO.
 */
@Schema(name = "ReceivableUpdateRequest",
        description = "Atualização parcial de uma conta a receber.")
public record ReceivableUpdateRequest(

        @Schema(description = "Descrição/origem da conta.",
                example = "SERVICO AVULSO DE MANUTENCAO",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 300)
        @Size(max = 300, message = "Descrição deve ter no máximo {max} caracteres")
        String description,

        @Schema(description = "Data de vencimento.",
                example = "2026-08-17", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Condição de pagamento (texto livre informativo).",
                example = "30/60/90", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 100)
        @Size(max = 100, message = "Condição de pagamento deve ter no máximo {max} caracteres")
        String paymentCondition
) {
}