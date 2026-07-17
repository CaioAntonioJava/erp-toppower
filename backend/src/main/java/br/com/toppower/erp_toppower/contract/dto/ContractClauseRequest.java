package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de uma cláusula de contrato enviados no corpo de
 * {@link ContractCreateRequest} ou {@link ContractUpdateRequest}.
 *
 * <p>O {@code serviceTemplateId} é preenchido apenas na cláusula 1
 * (DO OBJETO), referenciando o ServiceTemplate cuja descrição será
 * copiada para o {@code content}. Nas demais cláusulas fica nulo.</p>
 */
@Schema(name = "ContractClauseRequest", description = "Dados de uma cláusula de contrato.")
public record ContractClauseRequest(

        @Schema(description = "Número da cláusula (1 a 11).",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "O número da cláusula é obrigatório")
        Integer clauseNumber,

        @Schema(description = "Título da cláusula (ex.: \"DO OBJETO\").",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "DO OBJETO", maxLength = 200)
        @NotBlank(message = "O título da cláusula é obrigatório")
        @Size(max = 200, message = "O título deve ter no máximo {max} caracteres")
        String title,

        @Schema(description = "Texto completo da cláusula (HTML ou texto puro).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String content,

        @Schema(description = "ID do ServiceTemplate referenciado (apenas cláusula 1). "
                + "Quando informado, o backend copia a descrição do template para o content.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long serviceTemplateId
) {
}