package br.com.toppower.erp_toppower.user.dto;

import br.com.toppower.erp_toppower.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserResponse", description = "Representação pública de um usuário retornado pela API.")
public record UserResponse(

        @Schema(description = "Identificador único (ID) gerado pelo banco.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "E-mail do usuário.",
                example = "caio@toppower.com.br",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Papel do usuário.",
                example = "ROLE_MANAGER",
                allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        Role role
) {
}