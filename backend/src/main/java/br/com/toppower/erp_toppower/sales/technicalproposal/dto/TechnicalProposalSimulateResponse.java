package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta da simulação de totais de uma proposta técnica (sem
 * persistência).
 */
@Schema(name = "TechnicalProposalSimulateResponse",
        description = "Totais calculados de uma proposta técnica simulada.")
public record TechnicalProposalSimulateResponse(

        @Schema(description = "Itens da lista de serviços prestados (com UUID nulo, pois não persistidos).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<TechnicalProposalServiceItemResponse> serviceItems,

        @Schema(description = "Itens da lista de produtos (com UUID nulo, pois não persistidos).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<TechnicalProposalProductItemResponse> productItems,

        @Schema(description = "Soma dos preços dos serviços prestados.",
                example = "350.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal servicesSubtotal,

        @Schema(description = "Soma dos totais líquidos dos produtos.",
                example = "290.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal productsSubtotal,

        @Schema(description = "Subtotal geral (serviços + produtos).",
                example = "640.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal subtotal,

        @Schema(description = "Valor em R$ do desconto global efetivamente aplicado.",
                example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal globalDiscountValue,

        @Schema(description = "Total final (subtotal, menos desconto global, mais frete).",
                example = "649.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal total
) {
}