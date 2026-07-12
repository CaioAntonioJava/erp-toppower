package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Linha de cláusula de um contrato. Usado tanto no
 * {@code ContractCreateRequest} quanto no {@code ContractUpdateRequest}.
 */
@Schema(name = "ContractClauseRequest",
        description = "Linha de cláusula de um contrato.")
public record ContractClauseRequest(

        @Schema(description = "Descrição da cláusula contratual (texto livre).",
                example = "As partes acordam que...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Descrição da cláusula é obrigatória")
        @Size(max = 4000, message = "Cláusula deve ter no máximo {max} caracteres")
        String description
) {
}
