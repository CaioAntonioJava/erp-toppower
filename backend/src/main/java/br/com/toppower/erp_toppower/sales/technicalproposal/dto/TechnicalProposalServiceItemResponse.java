package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Linha de serviço retornada pela API. O preço é informado pelo usuário
 * (não há cálculo adicional sobre a linha). Quando o serviço é do catálogo,
 * os campos {@code category} e {@code serviceTemplateId} são preenchidos
 * para permitir a restauração do estado no formulário de edição.
 */
@Schema(name = "TechnicalProposalServiceItemResponse",
        description = "Linha de serviço de uma proposta técnica.")
public record TechnicalProposalServiceItemResponse(

        @Schema(description = "Identificador (UUID) da linha.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição do serviço prestado (HTML formatado). "
                + "Opcional — pode ser nula quando o serviço é informado apenas pelo preço.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @Schema(description = "Preço do serviço prestado. "
                + "Pode ser nulo quando o serviço é gratuito/incluso.",
                example = "350.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal price,

        @Schema(description = "Categoria do serviço no catálogo (ex.: \"EXECUÇÃO_SPDA\"). "
                + "Opcional — presente apenas quando o item é do catálogo.",
                example = "EXECUÇÃO_SPDA", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String category,

        @Schema(description = "ID do ServiceTemplate que originou este item. "
                + "Opcional — presente apenas quando o item é do catálogo.",
                example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long serviceTemplateId
) {
}