package br.com.toppower.erp_toppower.servicecategory.dto;

import br.com.toppower.erp_toppower.servicecategory.enums.ServiceCategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ServiceCategoryCreateRequest", description = "Dados para cadastro de uma nova categoria de serviço.")
public record ServiceCategoryCreateRequest(

        @Schema(description = "Nome da categoria de serviço.", example = "SPDA",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Status inicial. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ServiceCategoryStatus status
) {
}