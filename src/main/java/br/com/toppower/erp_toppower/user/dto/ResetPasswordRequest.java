package br.com.toppower.erp_toppower.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 8, max = 200, message = "Nova senha deve ter entre {min} e {max} caracteres")
        String newPassword
) {
}
