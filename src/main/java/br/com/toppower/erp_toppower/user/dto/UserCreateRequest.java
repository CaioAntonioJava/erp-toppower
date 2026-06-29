package br.com.toppower.erp_toppower.user.dto;

import br.com.toppower.erp_toppower.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UserCreateRequest", description = "Dados para cadastro de novo usuário.")
public record UserCreateRequest(

        @Schema(description = "E-mail único do usuário. Sera usado como credencial de login.",
                example = "caio@toppower.com.br", requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100)
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail invalido")
        @Size(max = 100, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Senha em texto puro. Sera codificada (BCrypt) antes de persistida.",
                example = "S3nh@Forte!", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8, maxLength = 200, format = "password")
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 200, message = "Senha deve ter entre {min} e {max} caracteres")
        String password,

        @Schema(description = "Papel atribuído ao usuário. Se omitido, assume ROLE_EMPLOYEE.",
                example = "ROLE_EMPLOYEE",
                allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Role role
) {
}
