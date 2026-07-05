package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linha de produto retornada pela API, com o total líquido já calculado
 * ({@code unitPrice * quantity - discount}).
 */
@Schema(name = "TechnicalProposalProductItemResponse",
        description = "Linha de produto de uma proposta técnica.")
public record TechnicalProposalProductItemResponse(

        @Schema(description = "Identificador (UUID) da linha.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Identificador (UUID) do produto referenciado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID productUuid,

        @Schema(description = "Quantidade do produto.", example = "2.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantity,

        @Schema(description = "Preço unitário do produto (snapshot).", example = "150.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal unitPrice,

        @Schema(description = "Subtotal bruto da linha (unitPrice * quantity), antes do desconto da linha.",
                example = "300.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal lineSubtotal,

        @Schema(description = "Tipo de aplicação do desconto da linha.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto da linha (R$ ou %).",
                example = "10.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal discount,

        @Schema(description = "Total líquido da linha (subtotal - desconto da linha). "
                + "Entra no subtotal da proposta.",
                example = "290.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalPrice
) {
}