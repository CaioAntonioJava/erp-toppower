package br.com.toppower.erp_toppower.userorganization.dto;

import br.com.toppower.erp_toppower.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "UserOrganizationResponse", description = "Vínculo usuário↔Organization.")
public record UserOrganizationResponse(

        @Schema(description = "ID do vínculo.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "ID do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @Schema(description = "E-mail do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
        String userEmail,

        @Schema(description = "ID da Organization.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long organizationId,

        @Schema(description = "Razão social da Organization.", requiredMode = Schema.RequiredMode.REQUIRED)
        String organizationCorporateName,

        @Schema(description = "Papel do usuário nesta Organization.", requiredMode = Schema.RequiredMode.REQUIRED)
        Role role,

        @Schema(description = "É a Organization default do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean isDefault,

        @Schema(description = "Data de criação.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt
) {
}