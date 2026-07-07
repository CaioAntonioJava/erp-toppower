package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Representação completa de uma proposta técnica retornada pela API,
 * incluindo itens (serviços e produtos) e totais calculados.
 */
@Schema(name = "TechnicalProposalResponse",
        description = "Representação completa de uma proposta técnica.")
public record TechnicalProposalResponse(

        @Schema(description = "Identificador único (UUID) da proposta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Prefixo do código (ex.: \"PL\").", example = "PL",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String prefix,

        @Schema(description = "Numeral sequencial do código.", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long sequence,

        @Schema(description = "Ano do código.", example = "2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer year,

        @Schema(description = "Código formatado completo (ex.: \"PL-001-2026\").",
                example = "PL-001-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "UUID do cliente pessoa física (presente quando o comprador for PF).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID customerUuid,

        @Schema(description = "UUID da empresa (presente quando o comprador for PJ).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID companyUuid,

        @Schema(description = "Tipo de cliente referenciado pela proposta.",
                allowableValues = {"CUSTOMER", "COMPANY"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ClientType clientType,

        @Schema(description = "Nome de exibição do cliente (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientName,

        @Schema(description = "Código interno do cliente (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientCode,

        @Schema(description = "Endereço de execução (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        TechnicalProposalAddressResponse address,

        @Schema(description = "Objetivos do serviço prestado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<TechnicalProposalObjectiveResponse> objectives,

        @Schema(description = "Descrição detalhada do serviço prestado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @Schema(description = "Status atual da proposta.",
                allowableValues = {"ABERTA", "EM_ANDAMENTO", "CONCLUIDA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        TechnicalProposalStatus status,

        @Schema(description = "Data de início.", example = "2026-07-05",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate startDate,

        @Schema(description = "Data de término prevista/real (informada manualmente).",
                example = "2026-07-15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate endDate,

        @Schema(description = "Data de entrega (preenchida ao concluir).",
                example = "2026-07-15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate deliveryDate,

        @Schema(description = "Itens da lista de serviços prestados.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<TechnicalProposalServiceItemResponse> serviceItems,

        @Schema(description = "Itens da lista de produtos.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<TechnicalProposalProductItemResponse> productItems,

        @Schema(description = "Margem de lucro aplicada sobre o subtotal dos itens (em %).",
                example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal profitMargin,

        @Schema(description = "Tipo de aplicação do desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto global.", example = "50.00",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal discount,

        @Schema(description = "Valor do frete (somado ao total após o desconto).",
                example = "45.90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal freightValue,

        @Schema(description = "Prazo de entrega (texto livre).", example = "3 dias",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String deliveryDeadline,

        @Schema(description = "Condição de pagamento.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Validade da proposta (texto livre).", example = "10 dias",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String validity,

        @Schema(description = "Tipo de entrega (CIF/FOB).",
                allowableValues = {"CIF", "FOB"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        FreightType deliveryType,

        @Schema(description = "Observações livres.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String notes,

        @Schema(description = "UUID da transportadora (Carrier) responsável pelo frete.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID carrierUuid,

        @Schema(description = "Nome da transportadora (resolvido no backend a partir de carrierUuid).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String carrierName,

        @Schema(description = "Soma dos preços dos serviços prestados.",
                example = "350.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal servicesSubtotal,

        @Schema(description = "Soma dos totais líquidos dos produtos.",
                example = "290.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal productsSubtotal,

        @Schema(description = "Subtotal geral (serviços + produtos), antes da margem e do desconto global.",
                example = "640.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal subtotal,

        @Schema(description = "Valor em R$ do desconto global efetivamente aplicado.",
                example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal globalDiscountValue,

        @Schema(description = "Total final (subtotal com margem, menos desconto global, mais frete).",
                example = "649.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal total,

        @Schema(description = "Data de criação do registro.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String updatedBy
) {

    /**
     * Tipo de cliente referenciado pela proposta (polimorfismo por
     * duas FKs nullable). Usado para indicar qual campo
     * ({@code customerUuid} ou {@code companyUuid}) está populado.
     */
    public enum ClientType {
        CUSTOMER,
        COMPANY
    }
}