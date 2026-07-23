package br.com.toppower.erp_toppower.user.dto;

import br.com.toppower.erp_toppower.user.enums.Module;
import br.com.toppower.erp_toppower.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

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
                allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        Role role,

        @Schema(description = "Módulos (paineis) efetivamente acessíveis ao usuário. " +
                "Para ROLE_ADMIN e ROLE_MANAGER retorna todos os módulos; para ROLE_EMPLOYEE " +
                "apenas os concedidos.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Set<Module> modules
) {
}