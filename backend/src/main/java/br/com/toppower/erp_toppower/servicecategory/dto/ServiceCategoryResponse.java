package br.com.toppower.erp_toppower.servicecategory.dto;

import br.com.toppower.erp_toppower.servicecategory.enums.ServiceCategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ServiceCategoryResponse", description = "Representação pública de uma categoria de serviço retornada pela API.")
public record ServiceCategoryResponse(

        @Schema(description = "Identificador único (ID) da categoria de serviço.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Nome da categoria de serviço.", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Status atual.", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ServiceCategoryStatus status,

        @Schema(description = "Data de criação.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.")
        String updatedBy
) {
}