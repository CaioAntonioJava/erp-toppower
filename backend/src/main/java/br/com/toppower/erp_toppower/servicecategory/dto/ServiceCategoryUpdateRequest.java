package br.com.toppower.erp_toppower.servicecategory.dto;

import br.com.toppower.erp_toppower.servicecategory.enums.ServiceCategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial da categoria de serviço (PATCH). Todos os campos são opcionais.
 */
@Schema(name = "ServiceCategoryUpdateRequest", description = "Dados para atualização parcial de uma categoria de serviço (PATCH).")
public record ServiceCategoryUpdateRequest(

        @Schema(description = "Novo nome da categoria de serviço.", maxLength = 100)
        @Size(max = 100, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Novo status.",
                allowableValues = {"ATIVO", "INATIVO"})
        ServiceCategoryStatus status
) {
}