package br.com.toppower.erp_toppower.user.dto;

import br.com.toppower.erp_toppower.user.enums.Module;
import br.com.toppower.erp_toppower.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(name = "UserCreateRequest", description = "Dados para cadastro de novo usuário.")
public record UserCreateRequest(

        @Schema(description = "E-mail único do usuário. Será usado como credencial de login.",
                example = "caio@toppower.com.br", requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100)
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 100, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Senha em texto puro. Será codificada (BCrypt) antes de persistida.",
                example = "S3nh@Forte!", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8, maxLength = 200, format = "password")
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 200, message = "Senha deve ter entre {min} e {max} caracteres")
        String password,

        @Schema(description = "Confirmação da senha. Deve ser idêntica a 'password'.",
                example = "S3nh@Forte!", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8, maxLength = 200, format = "password")
        @NotBlank(message = "Confirmação de senha é obrigatória")
        String passwordConfirmation,

        @Schema(description = "Papel do usuário no sistema.",
                example = "ROLE_EMPLOYEE",
                allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Papel é obrigatório")
        Role role,

        @Schema(description = "Módulos (paineis) acessíveis ao usuário. Relevantes apenas para " +
                "ROLE_EMPLOYEE; ROLE_ADMIN e ROLE_MANAGER recebem todos os módulos automaticamente.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Set<Module> modules
) {

    /**
     * Cross-field validation: garante que password e passwordConfirmation sejam iguais.
     * Lança MethodArgumentNotValidException → 400 via GlobalExceptionHandler.
     */
    @AssertTrue(message = "Senha e confirmação devem ser iguais")
    public boolean isPasswordsMatching() {
        return password != null && password.equals(passwordConfirmation);
    }
}
