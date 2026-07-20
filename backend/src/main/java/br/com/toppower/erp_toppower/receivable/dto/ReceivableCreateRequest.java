package br.com.toppower.erp_toppower.receivable.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados para cadastro manual de uma conta a receber. A origem
 * ({@code sourceType}) é sempre {@code MANUAL} para este endpoint.
 */
@Schema(name = "ReceivableCreateRequest",
        description = "Dados para cadastro manual de uma conta a receber.")
public record ReceivableCreateRequest(

        @Schema(description = "Descrição/origem da conta (ex.: 'Serviço avulso').",
                example = "SERVICO AVULSO DE MANUTENCAO",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 300)
        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 300, message = "Descrição deve ter no máximo {max} caracteres")
        String description,

        @Schema(description = "Valor total da conta.",
                example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        @Schema(description = "Data de vencimento.",
                example = "2026-08-17", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Data de vencimento é obrigatória")
        LocalDate dueDate,

        @Schema(description = "ID do cliente (pessoa física). Mutuamente exclusivo com companyId.",
                example = "12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long customerId,

        @Schema(description = "ID da empresa (pessoa jurídica). Mutuamente exclusivo com customerId.",
                example = "7", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long companyId,

        @Schema(description = "Condição de pagamento (mesmo domínio das propostas comerciais).",
                example = "PARCELAS_30_60_90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition
) {
}