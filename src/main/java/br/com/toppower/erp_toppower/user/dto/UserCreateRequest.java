package br.com.toppower.erp_toppower.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UserCreateRequest", description = "Dados para cadastro de novo usuario. " +
        "A role e definida automaticamente como ROLE_MANAGER no cadastro.")
public record UserCreateRequest(

        @Schema(description = "E-mail unico do usuario. Sera usado como credencial de login.",
                example = "caio@toppower.com.br", requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100)
        @NotBlank(message = "E-mail eh obrigatorio")
        @Email(message = "E-mail invalido")
        @Size(max = 100, message = "E-mail deve ter no maximo {max} caracteres")
        String email,

        @Schema(description = "Senha em texto puro. Sera codificada (BCrypt) antes de persistida.",
                example = "S3nh@Forte!", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8, maxLength = 200, format = "password")
        @NotBlank(message = "Senha eh obrigatoria")
        @Size(min = 8, max = 200, message = "Senha deve ter entre {min} e {max} caracteres")
        String password,

        @Schema(description = "Confirmacao da senha. Deve ser identica a 'password'.",
                example = "S3nh@Forte!", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8, maxLength = 200, format = "password")
        @NotBlank(message = "Confirmacao de senha eh obrigatoria")
        String passwordConfirmation
) {

    /**
     * Cross-field validation: garante que password e passwordConfirmation sejam iguais.
     * Lanca MethodArgumentNotValidException -> 400 via GlobalExceptionHandler.
     */
    @AssertTrue(message = "Senha e confirmacao devem ser iguais")
    public boolean isPasswordsMatching() {
        return password != null && password.equals(passwordConfirmation);
    }
}
