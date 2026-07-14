package br.com.toppower.erp_toppower.userorganization.dto;

import br.com.toppower.erp_toppower.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UserOrganizationAssignRequest",
        description = "Vincula um usuário a uma Organization com uma role de negócio.")
public record UserOrganizationAssignRequest(

        @Schema(description = "ID do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "userId é obrigatório")
        Long userId,

        @Schema(description = "ID da Organization.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "organizationId é obrigatório")
        Long organizationId,

        @Schema(description = "Papel do usuário nesta Organization.",
                example = "ROLE_MANAGER", allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "role é obrigatória")
        Role role,

        @Schema(description = "Indica se esta é a Organization default do usuário. "
                + "Se true, desmarca qualquer outra default do mesmo usuário.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean isDefault
) {
}