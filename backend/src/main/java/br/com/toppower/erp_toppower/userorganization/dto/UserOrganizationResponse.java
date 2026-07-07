package br.com.toppower.erp_toppower.userorganization.dto;

import br.com.toppower.erp_toppower.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "UserOrganizationResponse", description = "Vínculo usuário↔Organization.")
public record UserOrganizationResponse(

        @Schema(description = "UUID do vínculo.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "UUID do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID userUuid,

        @Schema(description = "E-mail do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
        String userEmail,

        @Schema(description = "UUID da Organization.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID organizationUuid,

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