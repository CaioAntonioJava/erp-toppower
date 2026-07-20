package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Linha de produto retornada pela API, com o total já calculado
 * ({@code unitPrice * quantity}).
 */
@Schema(name = "TechnicalProposalProductItemResponse",
        description = "Linha de produto de uma proposta técnica.")
public record TechnicalProposalProductItemResponse(

        @Schema(description = "Identificador (UUID) da linha.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Identificador (UUID) do produto referenciado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long productId,

        @Schema(description = "Quantidade do produto.", example = "2.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantity,

        @Schema(description = "Preço unitário do produto (snapshot no momento da emissão).",
                example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal unitPrice,

        @Schema(description = "Subtotal bruto da linha (unitPrice * quantity).",
                example = "300.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal lineSubtotal,

        @Schema(description = "Total da linha (unitPrice * quantity). Entra no subtotal da proposta.",
                example = "300.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalPrice
) {
}