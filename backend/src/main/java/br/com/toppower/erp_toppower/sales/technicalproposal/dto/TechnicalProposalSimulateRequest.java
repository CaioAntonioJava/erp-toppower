package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados para simulação de totais de uma proposta técnica sem persistir
 * nada. Variante <b>permissiva</b> do {@link TechnicalProposalCreateRequest}:
 * campos de identidade do cliente, datas e demais campos não-numéricos
 * não são exigidos — o preview pode ser disparado com o formulário em
 * estado intermediário.
 */
@Schema(name = "TechnicalProposalSimulateRequest",
        description = "Dados para simulação de totais de uma proposta técnica.")
public record TechnicalProposalSimulateRequest(

        @Schema(description = "Itens da lista de serviços prestados (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<TechnicalProposalServiceItemRequest> serviceItems,

        @Schema(description = "Itens da lista de produtos (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<TechnicalProposalProductItemRequest> productItems,

        @Schema(description = "Margem de lucro aplicada sobre o subtotal dos itens (em %).",
                example = "10.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Margem de lucro não pode ser negativa")
        @Digits(integer = 3, fraction = 2, message = "Margem de lucro inválida")
        BigDecimal profitMargin,

        @Schema(description = "Tipo de aplicação do desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto global.", example = "50.00",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
        BigDecimal discount,

        @Schema(description = "Valor do frete (somado ao total após o desconto).",
                example = "45.90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Frete inválido")
        BigDecimal freightValue,

        @Schema(description = "Tipo de entrega (CIF/FOB).",
                allowableValues = {"CIF", "FOB"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        FreightType deliveryType
) {
}