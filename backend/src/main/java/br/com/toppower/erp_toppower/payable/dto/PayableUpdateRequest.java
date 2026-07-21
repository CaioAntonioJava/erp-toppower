package br.com.toppower.erp_toppower.payable.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Atualização parcial (PATCH) de uma conta a pagar manual. Apenas
 * campos editáveis são expostos — valor, origem, fornecedor, parcelas
 * e vínculos com documentos de origem não são alteráveis. Bloqueada
 * para contas PAGO.
 */
@Schema(name = "PayableUpdateRequest",
        description = "Atualização parcial de uma conta a pagar.")
public record PayableUpdateRequest(

        @Schema(description = "Descrição/origem da conta.",
                example = "SERVICO AVULSO DE MANUTENCAO",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 300)
        @Size(max = 300, message = "Descrição deve ter no máximo {max} caracteres")
        String description,

        @Schema(description = "Data de emissão da conta.",
                example = "2026-07-21", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate issueDate,

        @Schema(description = "Vencimento-base da conta (1ª parcela).",
                example = "2026-08-17", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Condição de pagamento (mesmo domínio das propostas comerciais).",
                example = "PARCELAS_30_60_90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition
) {
}