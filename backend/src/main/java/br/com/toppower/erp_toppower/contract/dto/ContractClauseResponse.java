package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Linha de cláusula retornada pela API.
 */
@Schema(name = "ContractClauseResponse",
        description = "Linha de cláusula de um contrato.")
public record ContractClauseResponse(
        @Schema(description = "Identificador (UUID) da linha.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição da cláusula.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description
) {
}
