package br.com.toppower.erp_toppower.user.dto;

import br.com.toppower.erp_toppower.user.enums.Module;
import br.com.toppower.erp_toppower.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * Corpo de PATCH /api/v1/users/{id}. Apenas os campos informados são atualizados;
 * ausência (null) mantém o valor atual.
 */
@Schema(name = "UserUpdateRequest", description = "Dados para atualização de um usuário existente. " +
        "Campos ausentes (null) mantêm o valor atual.")
public record UserUpdateRequest(

        @Schema(description = "Novo papel do usuário. Ausente mantém o atual.",
                example = "ROLE_EMPLOYEE",
                allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Role role,

        @Schema(description = "Novo conjunto de módulos (paineis) acessíveis. " +
                "Relevante apenas para ROLE_EMPLOYEE; ROLE_ADMIN e ROLE_MANAGER recebem " +
                "todos os módulos automaticamente. Ausente mantém o conjunto atual.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Set<Module> modules
) {
}