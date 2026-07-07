package br.com.toppower.erp_toppower.organization.dto;

import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(name = "OrganizationUpdateRequest",
        description = "Dados para atualização de uma Organization. Todos os campos opcionais.")
public record OrganizationUpdateRequest(

        @Schema(description = "Razão social.", maxLength = 200,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 200, message = "Razão social deve ter no máximo {max} caracteres")
        String corporateName,

        @Schema(description = "Nome fantasia.", maxLength = 200,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 200, message = "Nome fantasia deve ter no máximo {max} caracteres")
        String tradeName,

        @Schema(description = "Inscrição estadual.", maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 50, message = "Inscrição estadual deve ter no máximo {max} caracteres")
        String stateRegistration,

        @Schema(description = "Inscrição municipal.", maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 50, message = "Inscrição municipal deve ter no máximo {max} caracteres")
        String municipalRegistration,

        @Schema(description = "Telefone.", maxLength = 20,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 20, message = "Telefone deve ter no máximo {max} caracteres")
        String phone,

        @Schema(description = "E-mail.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Email(message = "E-mail inválido")
        @Size(max = 100, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "CEP.", maxLength = 9, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 9, message = "CEP deve ter no máximo {max} caracteres")
        String zipCode,

        @Schema(description = "Logradouro.", maxLength = 200,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 200, message = "Logradouro deve ter no máximo {max} caracteres")
        String street,

        @Schema(description = "Número.", maxLength = 20, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 20, message = "Número deve ter no máximo {max} caracteres")
        String number,

        @Schema(description = "Bairro.", maxLength = 100, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "Bairro deve ter no máximo {max} caracteres")
        String district,

        @Schema(description = "Cidade.", maxLength = 100, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "Cidade deve ter no máximo {max} caracteres")
        String city,

        @Schema(description = "UF (2 letras).", maxLength = 2, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 2, message = "UF deve ter no máximo {max} caracteres")
        String state,

        @Schema(description = "Complemento.", maxLength = 100, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "Complemento deve ter no máximo {max} caracteres")
        String complement,

        @Schema(description = "Status.", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        OrganizationStatus status
) {
}