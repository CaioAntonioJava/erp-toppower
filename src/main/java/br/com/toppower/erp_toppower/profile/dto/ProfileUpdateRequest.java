package br.com.toppower.erp_toppower.profile.dto;

import br.com.toppower.erp_toppower.profile.enums.ProfileStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial do perfil (PATCH). Todos os campos são opcionais:
 * envie apenas os campos que deseja alterar. O vínculo com o usuário
 * (userId) NÃO pode ser alterado por este endpoint.
 */
@Schema(name = "ProfileUpdateRequest", description = "Dados para atualização parcial de um perfil (PATCH).")
public record ProfileUpdateRequest(

        @Schema(description = "Novo nome completo.", maxLength = 150)
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Novo e-mail de contato.", maxLength = 150)
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Novo telefone de contato.", maxLength = 20)
        @Size(max = 20, message = "Telefone deve ter no máximo {max} caracteres")
        String phone,

        @Schema(description = "Novo CPF.", maxLength = 14)
        @Size(max = 14, message = "CPF deve ter no máximo {max} caracteres")
        @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}",
                message = "CPF deve estar no formato 000.000.000-00 ou conter 11 dígitos")
        String cpf,

        @Schema(description = "Novo status do perfil.",
                allowableValues = {"ATIVO", "INATIVO"})
        ProfileStatus status
) {
}
