package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linha de serviço retornada pela API. O preço é informado pelo usuário
 * (não há cálculo adicional sobre a linha).
 */
@Schema(name = "TechnicalProposalServiceItemResponse",
        description = "Linha de serviço de uma proposta técnica.")
public record TechnicalProposalServiceItemResponse(

        @Schema(description = "Identificador (UUID) da linha.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Descrição do serviço prestado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Preço final do serviço (snapshot, com margem de lucro embutida). "
                + "Pode ser nulo quando o serviço é gratuito/incluso.",
                example = "350.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal price,

        @Schema(description = "Preço original enviado pelo usuário (sem margem de lucro). "
                + "Pode ser nulo quando o serviço é gratuito/incluso. É o valor que o "
                + "formulário usa como ponto de partida na edição — aplicar a margem "
                + "sobre o `price` causaria duplicação.",
                example = "304.35", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal basePrice
) {
}