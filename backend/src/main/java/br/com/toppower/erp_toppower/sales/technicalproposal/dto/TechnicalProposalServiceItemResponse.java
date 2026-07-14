package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Linha de serviço retornada pela API. O preço é informado pelo usuário
 * (não há cálculo adicional sobre a linha).
 */
@Schema(name = "TechnicalProposalServiceItemResponse",
        description = "Linha de serviço de uma proposta técnica.")
public record TechnicalProposalServiceItemResponse(

        @Schema(description = "Identificador (UUID) da linha.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição do serviço prestado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Preço do serviço prestado. "
                + "Pode ser nulo quando o serviço é gratuito/incluso.",
                example = "350.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal price
) {
}