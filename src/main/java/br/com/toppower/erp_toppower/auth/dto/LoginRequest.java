package br.com.toppower.erp_toppower.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "Credenciais para autenticação do usuário via POST /auth/login.")
public record LoginRequest(

        @Schema(description = "E-mail do usuário cadastrado.",
                example = "caio@toppower.com.br", requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100)
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @Schema(description = "Senha em texto puro. Mínimo 8 caracteres.",
                example = "S3nh@Forte!", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8, maxLength = 200, format = "password")
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 200, message = "Senha deve ter entre {min} e {max} caracteres")
        String password
) {
}
