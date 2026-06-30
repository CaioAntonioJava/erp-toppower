package br.com.toppower.erp_toppower.profile.dto;

import br.com.toppower.erp_toppower.profile.enums.ProfileStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "ProfileCreateRequest", description = "Dados para cadastro do proprio perfil. " +
        "O vinculo com o usuario eh feito automaticamente a partir do JWT (1:1).")
public record ProfileCreateRequest(

        @Schema(description = "Nome completo da pessoa.", example = "Caio Antonio da Silva",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank(message = "Nome eh obrigatorio")
        @Size(max = 150, message = "Nome deve ter no maximo {max} caracteres")
        String name,

        @Schema(description = "E-mail de contato do perfil (deve ser unico entre perfis).",
                example = "caio@toppower.com.br", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank(message = "E-mail eh obrigatorio")
        @Email(message = "E-mail invalido")
        @Size(max = 150, message = "E-mail deve ter no maximo {max} caracteres")
        String email,

        @Schema(description = "Telefone de contato.", example = "(11) 98765-4321",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
        @NotBlank(message = "Telefone eh obrigatorio")
        @Size(max = 20, message = "Telefone deve ter no maximo {max} caracteres")
        String phone,

        @Schema(description = "CPF unico (somente digitos ou formatado).",
                example = "123.456.789-00",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 14)
        @NotBlank(message = "CPF eh obrigatorio")
        @Size(max = 14, message = "CPF deve ter no maximo {max} caracteres")
        @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}",
                message = "CPF deve estar no formato 000.000.000-00 ou conter 11 digitos")
        String cpf,

        @Schema(description = "Status inicial do perfil. Se omitido, assume ATIVO.",
                example = "ATIVO",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ProfileStatus status
) {
}
