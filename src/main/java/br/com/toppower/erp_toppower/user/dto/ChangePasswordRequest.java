package br.com.toppower.erp_toppower.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ChangePasswordRequest", description = "Troca de senha do proprio usuario autenticado.")
public record ChangePasswordRequest(

        @Schema(description = "Senha atual do usuario. Necessaria para autorizar a troca.",
                example = "S3nh@Atual!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Senha atual eh obrigatoria")
        String currentPassword,

        @Schema(description = "Nova senha que substituira a atual. Minimo 8 caracteres.",
                example = "N0v@Senh@2026", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8, maxLength = 200, format = "password")
        @NotBlank(message = "Nova senha eh obrigatoria")
        @Size(min = 8, max = 200, message = "Nova senha deve ter entre {min} e {max} caracteres")
        String newPassword
) {
}
