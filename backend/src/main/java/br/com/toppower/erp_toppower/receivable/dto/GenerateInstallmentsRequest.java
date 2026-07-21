package br.com.toppower.erp_toppower.receivable.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload para gerar parcelas programadas em uma conta a receber
 * existente (ação pós-criação, disparada pelo botão "Gerar parcelas").
 *
 * <p>Regras de uso (validadas pelo service):</p>
 * <ul>
 *   <li>A conta deve estar ABERTO, com {@code installmentsCount == 1},
 *       {@code paidAmount == 0} e nenhum pagamento registrado;</li>
 *   <li>Se {@code installments} explícitas informadas → usa-as
 *       diretamente (a soma deve bater com o valor da conta);</li>
 *   <li>Se {@code paymentCondition} informada e sem parcelas explícitas
 *       → gera parcelas automáticas a partir dos prazos da condição
 *       (ex.: PARCELAS_30_60_90 → 3 parcelas com vencimentos
 *       baseDate+30, baseDate+60, baseDate+90);</li>
 *   <li>Se {@code baseDate} omitida → usa a data de vencimento-base da
 *       conta como referência para o cálculo dos vencimentos;</li>
 *   <li>Se {@code paymentCondition} e {@code installments} ambos
 *       omitidos → lança erro de validação.</li>
 * </ul>
 */
@Schema(name = "GenerateInstallmentsRequest",
        description = "Payload para gerar parcelas programadas em uma conta a receber existente.")
public record GenerateInstallmentsRequest(

        @Schema(description = "Condição de pagamento usada para gerar as parcelas. "
                + "Mutuamente exclusivo com 'installments'.",
                example = "PARCELAS_30_60_90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Data-base para o cálculo dos vencimentos (ex.: data de emissão "
                + "ou vencimento-base da conta). Default: vencimento-base da conta.",
                example = "2026-07-21", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate baseDate,

        @Schema(description = "Parcelas explícitas (valor + vencimento). Mutuamente exclusivo "
                + "com 'paymentCondition'. A soma deve bater com o valor da conta.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ReceivableInstallmentRequest> installments
) {
}