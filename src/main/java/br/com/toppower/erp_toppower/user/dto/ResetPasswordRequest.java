package br.com.toppower.erp_toppower.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ResetPasswordRequest", description = "Reset de senha de um usuário pelo ADMIN.")
public record ResetPasswordRequest(

        @Schema(description = "Nova senha a ser atribuida ao usuário. Mínimo 8 caracteres.",
                example = "T3mp@Senha!2026", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8, maxLength = 200, format = "password")
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 8, max = 200, message = "Nova senha deve ter entre {min} e {max} caracteres")
        String newPassword
) {
}
