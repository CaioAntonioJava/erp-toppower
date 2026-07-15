package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Condição de uma proposta técnica retornada pela API.
 */
@Schema(name = "TechnicalProposalConditionResponse",
        description = "Condição de uma proposta técnica.")
public record TechnicalProposalConditionResponse(

        @Schema(description = "Identificador da condição.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Título da condição (ex.: \"Garantia\", \"Prazo de pagamento\").",
                example = "Garantia",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "Conteúdo textual da condição. Opcional.",
                example = "12 meses contra defeitos de fabricação.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String content,

        @Schema(description = "Ordem de exibição da condição na lista.",
                example = "0",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer sortOrder
) {
}
