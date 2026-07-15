package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Item da lista de condições de uma proposta técnica. Usado tanto no
 * {@code TechnicalProposalCreateRequest} quanto no
 * {@code TechnicalProposalUpdateRequest}.
 *
 * <p>Cada condição possui um título (obrigatório) e um conteúdo textual
 * (opcional). A ordem de exibição é definida pela posição do item na lista.</p>
 */
@Schema(name = "TechnicalProposalConditionRequest",
        description = "Condição de uma proposta técnica.")
public record TechnicalProposalConditionRequest(

        @Schema(description = "Título da condição (ex.: \"Garantia\", \"Prazo de pagamento\").",
                example = "Garantia",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Título da condição é obrigatório")
        @Size(max = 150, message = "Título da condição deve ter no máximo {max} caracteres")
        String title,

        @Schema(description = "Conteúdo textual da condição. Opcional.",
                example = "12 meses contra defeitos de fabricação.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 5000, message = "Conteúdo da condição deve ter no máximo {max} caracteres")
        String content
) {
}
