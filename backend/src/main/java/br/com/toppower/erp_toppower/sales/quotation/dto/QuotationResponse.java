package br.com.toppower.erp_toppower.sales.quotation.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Representação completa de uma proposta comercial retornada pela API,
 * incluindo itens e totais calculados.
 */
@Schema(name = "QuotationResponse", description = "Representação completa de uma proposta comercial.")
public record QuotationResponse(

        @Schema(description = "Identificador único (UUID) da proposta.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Número sequencial da proposta (sem prefixo).", example = "1500",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long number,

        @Schema(description = "Data de emissão da proposta.", example = "2026-07-02",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate issueDate,

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

        @Schema(description = "Nome de exibição do cliente (PF: nome; PJ: nome fantasia se houver, senão razão social). "
                + "Resolvido no backend a partir do UUID referenciado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientName,

        @Schema(description = "Código interno do cliente (ex.: \"CLI000001\", \"EMP000001\"). "
                + "Resolvido no backend a partir do UUID referenciado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientCode,

        @Schema(description = "Aos cuidados de.", example = "Sr. João Silva",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String attention,

        @Schema(description = "UUID do vendedor.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID sellerUuid,

        @Schema(description = "Nome do vendedor (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String sellerName,

        @Schema(description = "Itens da proposta.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<QuotationItemResponse> items,

        @Schema(description = "Tipo de aplicação do desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto global.", example = "50.00",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal discount,

        @Schema(description = "Prazo de validade em dias.", example = "15",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer validityDays,

        @Schema(description = "Condição de pagamento.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Observações livres.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String notes,

        @Schema(description = "Tipo de frete.",
                allowableValues = {"CIF", "FOB"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        FreightType freightType,

        @Schema(description = "Valor do frete (somado ao total após o desconto global).",
                example = "45.90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal freightValue,

        @Schema(description = "UUID da transportadora (Carrier) responsável pelo frete.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID carrierUuid,

        @Schema(description = "Nome da transportadora (resolvido no backend a partir de carrierUuid).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String carrierName,

        @Schema(description = "Nome do serviço da transportadora (resolvido no backend a partir de carrierUuid).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String carrierServiceName,

        @Schema(description = "Margem de lucro aplicada sobre o total da proposta (em %). "
                + "Ex.: 10.00 = 10% aplicado como multiplicação sobre o total parcial.",
                example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal profitMargin,

        @Schema(description = "Status atual da proposta.",
                allowableValues = {"ATIVA", "CONVERTIDA", "CANCELADA", "EXPIRADA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        QuotationStatus status,

        @Schema(description = "Soma dos totais líquidos dos itens (após descontos por item), antes do desconto global.",
                example = "1450.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal subtotal,

        @Schema(description = "Total final da proposta (subtotal com margem de lucro aplicada, menos o desconto global, mais o frete).",
                example = "1590.49", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal total,

        @Schema(description = "Valor em R$ do desconto global efetivamente aplicado (já considerando a margem de lucro sobre o subtotal).",
                example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal globalDiscountValue,

        @Schema(description = "Soma das quantidades de todos os itens (unidades comercializadas).",
                example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer totalQuantity,

        @Schema(description = "Data de criação do registro.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
