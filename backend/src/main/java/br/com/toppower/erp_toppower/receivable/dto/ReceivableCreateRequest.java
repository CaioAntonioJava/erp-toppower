package br.com.toppower.erp_toppower.receivable.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dados para cadastro manual de uma conta a receber. A origem
 * ({@code sourceType}) é sempre {@code MANUAL} para este endpoint.
 *
 * <p><b>Parcelamento</b>: o usuário pode informar a lista de parcelas
 * explícitas (valor + vencimento de cada uma) OU omiti-la — nesse
 * caso, é criada uma única parcela com o valor total e o vencimento
 * {@code dueDate}. Alternativamente, ao informar apenas
 * {@code paymentCondition} com múltiplos prazos (ex.: PARCELAS_30_60_90),
 * o service gera as parcelas automaticamente a partir dos prazos da
 * condição, usando {@code issueDate} como base. A soma das parcelas
 * deve bater com {@code value}.</p>
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

        @Schema(description = "Valor total da conta (soma das parcelas).",
                example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        @Schema(description = "Data de emissão da conta. Base para o cálculo dos vencimentos "
                + "das parcelas quando a condição de pagamento é informada em vez das "
                + "datas explícitas. Default: hoje.",
                example = "2026-07-21", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate issueDate,

        @Schema(description = "Vencimento-base (1ª parcela). Usado quando a lista de "
                + "parcelas não é informada.", example = "2026-08-17",
                requiredMode = Schema.RequiredMode.REQUIRED)
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
        PaymentCondition paymentCondition,

        @Schema(description = "Parcelas programadas. Quando omitido, é criada uma única "
                + "parcela com o valor total e o vencimento 'dueDate'. A soma das parcelas "
                + "deve bater com 'value'.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ReceivableInstallmentRequest> installments
) {
}