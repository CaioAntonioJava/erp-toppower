package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Linha de objetivo retornada pela API.
 */
@Schema(name = "TechnicalProposalObjectiveResponse",
        description = "Linha de objetivo de uma proposta técnica.")
public record TechnicalProposalObjectiveResponse(
        @Schema(description = "Identificador (UUID) da linha.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição do objetivo.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description
) {
}