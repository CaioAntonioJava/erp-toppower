package br.com.toppower.erp_toppower.receivable.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload para o preview de parcelas a partir de uma condição de
 * pagamento e um valor total. Não persiste — apenas calcula e retorna
 * a lista de parcelas que seriam geradas.
 */
@Schema(name = "PreviewInstallmentsRequest",
        description = "Payload para preview de parcelas a partir da condição de pagamento.")
public record PreviewInstallmentsRequest(

        @Schema(description = "Condição de pagamento.",
                example = "PARCELAS_30_60_90", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Condição de pagamento é obrigatória")
        PaymentCondition paymentCondition,

        @Schema(description = "Valor total a ser distribuído entre as parcelas.",
                example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        @Schema(description = "Data-base para o cálculo dos vencimentos. Default: hoje.",
                example = "2026-07-21", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate baseDate
) {
}