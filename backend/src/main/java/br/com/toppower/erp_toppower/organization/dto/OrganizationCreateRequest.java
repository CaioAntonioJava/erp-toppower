package br.com.toppower.erp_toppower.organization.dto;

import br.com.toppower.erp_toppower.common.validation.ValidCnpj;
import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "OrganizationCreateRequest", description = "Dados para cadastro de uma nova Organization (empresa).")
public record OrganizationCreateRequest(

        @Schema(description = "Razão social.", example = "TOP POWER ENGENHARIA LTDA",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 200, message = "Razão social deve ter no máximo {max} caracteres")
        String corporateName,

        @Schema(description = "Nome fantasia.", example = "TOP POWER ENGENHARIA",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Nome fantasia é obrigatório")
        @Size(max = 200, message = "Nome fantasia deve ter no máximo {max} caracteres")
        String tradeName,

        @Schema(description = "CNPJ (com ou sem formatação).",
                example = "12.345.678/0001-90", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "CNPJ é obrigatório")
        @ValidCnpj
        String cnpj,

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

        @Schema(description = "E-mail.", example = "contato@toppower.com.br",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Email(message = "E-mail inválido")
        @Size(max = 100, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "CEP (8 dígitos, com ou sem traço).", maxLength = 9,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 9, message = "CEP deve ter no máximo {max} caracteres")
        String zipCode,

        @Schema(description = "Logradouro.", maxLength = 200,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 200, message = "Logradouro deve ter no máximo {max} caracteres")
        String street,

        @Schema(description = "Número.", maxLength = 20,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 20, message = "Número deve ter no máximo {max} caracteres")
        String number,

        @Schema(description = "Bairro.", maxLength = 100,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "Bairro deve ter no máximo {max} caracteres")
        String district,

        @Schema(description = "Cidade.", maxLength = 100,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "Cidade deve ter no máximo {max} caracteres")
        String city,

        @Schema(description = "UF (2 letras).", maxLength = 2,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 2, message = "UF deve ter no máximo {max} caracteres")
        String state,

        @Schema(description = "Complemento.", maxLength = 100,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "Complemento deve ter no máximo {max} caracteres")
        String complement,

        @Schema(description = "URL pública do logo da Organization "
                + "(ex.: /logos/<uuid>.png). Geralmente preenchida após o "
                + "upload via endpoint dedicado /organizations/{id}/logo.",
                maxLength = 500, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 500, message = "URL do logo deve ter no máximo {max} caracteres")
        String logoUrl,

        @Schema(description = "Status inicial. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        OrganizationStatus status,

        @Schema(description = "Prefixo do código das Propostas Técnicas emitidas "
                + "por esta Organization (ex.: 'PT' para Engenharia, 'PL' para "
                + "Materiais). Obrigatório e único no sistema.",
                example = "PT", maxLength = 10,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Prefixo de proposta é obrigatório")
        @Size(max = 10, message = "Prefixo de proposta deve ter no máximo {max} caracteres")
        String proposalPrefix
) {
}