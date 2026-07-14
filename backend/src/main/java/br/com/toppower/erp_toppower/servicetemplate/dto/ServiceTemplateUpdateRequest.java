package br.com.toppower.erp_toppower.servicetemplate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial do serviço (PATCH). Todos os campos são opcionais.
 */
@Schema(name = "ServiceTemplateUpdateRequest", description = "Dados para atualização parcial de um serviço (PATCH).")
public record ServiceTemplateUpdateRequest(

        @Schema(description = "Novo nome do serviço.", maxLength = 200)
        @Size(max = 200, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Nova descrição do serviço.", maxLength = 500)
        @Size(max = 500, message = "Descrição deve ter no máximo {max} caracteres")
        String description
) {
}
