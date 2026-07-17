package br.com.toppower.erp_toppower.servicetemplate.dto;

import br.com.toppower.erp_toppower.servicetemplate.enums.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ServiceTemplateResponse", description = "Representação pública de um serviço retornado pela API.")
public record ServiceTemplateResponse(

        @Schema(description = "Identificador único (ID) do serviço.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição detalhada do serviço.")
        String description,

        @Schema(description = "Categoria do serviço.", requiredMode = Schema.RequiredMode.REQUIRED)
        ServiceCategory category,

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
