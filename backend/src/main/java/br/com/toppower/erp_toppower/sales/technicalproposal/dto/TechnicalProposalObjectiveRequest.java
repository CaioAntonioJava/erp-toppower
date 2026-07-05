package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Linha de objetivo de uma proposta técnica. Usado tanto no
 * {@code TechnicalProposalCreateRequest} quanto no
 * {@code TechnicalProposalUpdateRequest}.
 */
@Schema(name = "TechnicalProposalObjectiveRequest",
        description = "Linha de objetivo de uma proposta técnica.")
public record TechnicalProposalObjectiveRequest(

        @Schema(description = "Descrição do objetivo do serviço prestado (texto livre).",
                example = "Substituição de quadro de distribuição.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Descrição do objetivo é obrigatória")
        @Size(max = 500, message = "Descrição do objetivo deve ter no máximo {max} caracteres")
        String description
) {
}