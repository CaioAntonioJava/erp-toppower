package br.com.toppower.erp_toppower.servicetemplate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ServiceTemplateResponse", description = "Representação pública de um serviço retornado pela API.")
public record ServiceTemplateResponse(

        @Schema(description = "Identificador único (ID) do serviço.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Nome do serviço.", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Descrição detalhada do serviço.")
        String description,

        @Schema(description = "ID da categoria de serviço.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long categoryId,

        @Schema(description = "Nome da categoria de serviço.", requiredMode = Schema.RequiredMode.REQUIRED)
        String categoryName,

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