package br.com.toppower.erp_toppower.user.dto;

import br.com.toppower.erp_toppower.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "UserResponse", description = "Representacao publica de um usuario retornado pela API.")
public record UserResponse(

        @Schema(description = "Identificador unico (UUID) gerado pelo banco.",
                example = "5b6f2a1c-4c8d-4f3a-9b7e-1d2e3f4a5b6c",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "E-mail do usuario.",
                example = "caio@toppower.com.br",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Papel do usuario.",
                example = "ROLE_EMPLOYEE",
                allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        Role role
) {
}
