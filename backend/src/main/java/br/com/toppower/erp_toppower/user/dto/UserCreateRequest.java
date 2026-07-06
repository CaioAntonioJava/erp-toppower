package br.com.toppower.erp_toppower.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(name = "UserCreateRequest", description = "Dados para cadastro de novo usuário. " +
        "A role é definida automaticamente como ROLE_MANAGER no cadastro. O admin seleciona " +
        "a quais empresas (tenants) o usuário terá acesso — ao menos uma é obrigatória.")
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

        @Schema(description = "Lista de UUIDs das empresas (tenants) às quais o usuário terá acesso. " +
                "Ao menos uma deve ser informada.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Selecione ao menos uma empresa")
        List<UUID> tenantUuids
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
