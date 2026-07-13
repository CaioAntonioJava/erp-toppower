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
 * Dados para criação de uma nova proposta técnica.
 *
 * <p>O código comercial ({@code PL-001-2026}) é gerado automaticamente
 * pelo servidor: prefixo fixo {@code PL}, sequência reiniciando a
 * {@code 1} a cada novo ano, e ano corrente. O status inicial é
 * {@code ABERTA} e a data de início, se não informada, recebe
 * {@code LocalDate.now()} na persistência.</p>
 *
 * <p>Deve ser informado exatamente <b>um</b> entre {@link #customerUuid}
 * (cliente pessoa física) e {@link #companyUuid} (cliente pessoa
 * jurídica). A validação dessa invariante é feita no serviço.</p>
 *
 * <p>Ao menos um item (serviço ou produto) deve ser informado — a
 * proposta técnica não pode ser criada sem nenhum item.</p>
 */
@Schema(name = "TechnicalProposalCreateRequest",
        description = "Dados para cadastro de uma nova proposta técnica.")
public record TechnicalProposalCreateRequest(

        @Schema(description = "UUID do cliente pessoa física. OBRIGATÓRIO se companyUuid não for informado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID customerUuid,

        @Schema(description = "UUID da empresa (pessoa jurídica). OBRIGATÓRIO se customerUuid não for informado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID companyUuid,

        @Schema(description = "Endereço de execução (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        TechnicalProposalAddressRequest address,

        @Schema(description = "Objetivos do serviço prestado — descrições curtas. A proposta deve ter ao menos um objetivo.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "A proposta deve ter ao menos um objetivo")
        @Valid
        List<TechnicalProposalObjectiveRequest> objectives,

        @Schema(description = "Descrição detalhada do serviço prestado (formalização).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @Schema(description = "Nome do responsável técnico pela proposta. Opcional.",
                example = "João da Silva",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 150, message = "Responsável técnico deve ter no máximo {max} caracteres")
        String technicalResponsible,

        @Schema(description = "E-mail de contato do responsável técnico. Opcional — campo livre, sem validação de formato.",
                example = "joao.silva@empresa.com",
                maxLength = 200,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 200, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Data de início da proposta (yyyy-MM-dd). Se omitida, usa a data atual.",
                example = "2026-07-05", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate startDate,

        @Schema(description = "Data de término prevista/real do serviço (yyyy-MM-dd). Opcional — informada manualmente.",
                example = "2026-07-15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate endDate,

        @Schema(description = "Itens da lista de serviços prestados. Opcional, mas ao menos um item "
                + "(serviço ou produto) deve ser informado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<TechnicalProposalServiceItemRequest> serviceItems,

        @Schema(description = "Itens da lista de produtos. Opcional, mas ao menos um item "
                + "(serviço ou produto) deve ser informado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<TechnicalProposalProductItemRequest> productItems,

        @Schema(description = "Tipo de aplicação do desconto global (AMOUNT = R$ fixo, PERCENT = %). "
                + "Quando omitido, a proposta não tem desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto global, interpretado conforme discountType.",
                example = "50.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
        BigDecimal discount,

        @Schema(description = "Valor do frete informado manualmente. Somado ao total após o desconto global.",
                example = "45.90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Frete inválido")
        BigDecimal freightValue,

        @Schema(description = "Prazo de entrega em texto livre (ex.: \"3 dias\").",
                example = "3 dias", maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 50, message = "Prazo de entrega deve ter no máximo {max} caracteres")
        String deliveryDeadline,

        @Schema(description = "Condição de pagamento (mesmo domínio das propostas comerciais).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Validade da proposta em texto livre (ex.: \"10 dias\").",
                example = "10 dias", maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 50, message = "Validade deve ter no máximo {max} caracteres")
        String validity,

        @Schema(description = "Tipo de entrega (CIF/FOB), mesmo domínio usado em cotação.",
                allowableValues = {"CIF", "FOB"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        FreightType deliveryType,

        @Schema(description = "Observações livres da proposta.", maxLength = 2000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 2000, message = "Observações devem ter no máximo {max} caracteres")
        String notes,

        @Schema(description = "UUID da transportadora (Carrier) responsável pelo frete. Opcional.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID carrierUuid
) {
}