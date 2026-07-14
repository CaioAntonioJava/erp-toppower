package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Item de produto de um contrato (response).
 */
@Schema(name = "ContractProductItemResponse",
        description = "Item de produto de um contrato.")
public record ContractProductItemResponse(

        @Schema(description = "Identificador único do item.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "UUID do produto referenciado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long productId,

        @Schema(description = "Quantidade contratada.",
                example = "10.0000", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantity
) {
}
