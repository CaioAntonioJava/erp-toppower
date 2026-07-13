package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Atualização parcial (PATCH) de uma proposta técnica.
 *
 * <p>Todos os campos são opcionais: envie apenas os que deseja alterar.
 * As listas de itens (serviços/produtos) são tratadas como <b>substituição
 * completa</b>: ao enviar uma nova lista, os itens anteriores são
 * removidos e os novos são criados.</p>
 *
 * <p>O código comercial ({@code PL-001-2026}), a data de criação e o
 * status da proposta <b>não</b> podem ser alterados por este request.
 * Use os endpoints dedicados para transições de status
 * ({@code /start}, {@code /complete}, {@code /cancel}).</p>
 */
@Schema(name = "TechnicalProposalUpdateRequest",
        description = "Dados para atualização parcial de uma proposta técnica (PATCH).")
public record TechnicalProposalUpdateRequest(

        @Schema(description = "Novo UUID do cliente pessoa física.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID customerUuid,

        @Schema(description = "Novo UUID da empresa (pessoa jurídica).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID companyUuid,

        @Schema(description = "Novo endereço de execução. Envie nulo para remover.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        TechnicalProposalAddressRequest address,

        @Schema(description = "Nova lista de objetivos (substitui a anterior por completo). Se informada, deve conter ao menos um item.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @NotEmpty(message = "A lista de objetivos não pode ser vazia quando informada")
        @Valid
        List<TechnicalProposalObjectiveRequest> objectives,

        @Schema(description = "Nova descrição detalhada do serviço prestado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @Schema(description = "Novo nome do responsável técnico. Envie string vazia para limpar.",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 150, message = "Responsável técnico deve ter no máximo {max} caracteres")
        String technicalResponsible,

        @Schema(description = "Novo e-mail do responsável técnico. Envie string vazia para limpar. Campo livre, sem validação de formato.",
                maxLength = 200,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 200, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Nova data de início (yyyy-MM-dd).",
                example = "2026-07-05", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate startDate,

        @Schema(description = "Nova data de término (yyyy-MM-dd). Envie nulo para remover.",
                example = "2026-07-15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate endDate,

        @Schema(description = "Nova lista de serviços (substitui a anterior por completo). "
                + "Se informada, o service valida a consistência.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<TechnicalProposalServiceItemRequest> serviceItems,

        @Schema(description = "Nova lista de produtos (substitui a anterior por completo). "
                + "Se informada, o service valida a consistência.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<TechnicalProposalProductItemRequest> productItems,

        @Schema(description = "Novo tipo de desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Novo valor do desconto global.",
                example = "50.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
        BigDecimal discount,

        @Schema(description = "Novo valor do frete (somado ao total após o desconto).",
                example = "45.90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Frete inválido")
        BigDecimal freightValue,

        @Schema(description = "Novo prazo de entrega (texto livre).", example = "3 dias",
                maxLength = 50, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 50, message = "Prazo de entrega deve ter no máximo {max} caracteres")
        String deliveryDeadline,

        @Schema(description = "Nova condição de pagamento.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Nova validade da proposta (texto livre).", example = "10 dias",
                maxLength = 50, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 50, message = "Validade deve ter no máximo {max} caracteres")
        String validity,

        @Schema(description = "Novo tipo de entrega (CIF/FOB).",
                allowableValues = {"CIF", "FOB"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        FreightType deliveryType,

        @Schema(description = "Novas observações livres (enviar string vazia para limpar).",
                maxLength = 2000, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 2000, message = "Observações devem ter no máximo {max} caracteres")
        String notes,

        @Schema(description = "Nova transportadora (Carrier) responsável pelo frete. "
                + "Envie nulo para remover a transportadora vinculada.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID carrierUuid
) {
}