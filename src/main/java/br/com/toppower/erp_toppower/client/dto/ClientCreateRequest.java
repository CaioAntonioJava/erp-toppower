package br.com.toppower.erp_toppower.client.dto;

import br.com.toppower.erp_toppower.client.enums.ClientStatus;
import br.com.toppower.erp_toppower.client.enums.PersonType;
import br.com.toppower.erp_toppower.common.validation.ValidTaxId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "ClientCreateRequest", description = "Dados para cadastro de um novo cliente (PF ou PJ).")
@ValidTaxId(taxIdField = "taxId", personTypeField = "personType")
public record ClientCreateRequest(

        @Schema(description = "Razão social (nome oficial/registrado).",
                example = "Empresa XPTO Ltda", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 200, message = "Razão social deve ter no máximo {max} caracteres")
        String legalName,

        @Schema(description = "Nome fantasia (nome comercial).", example = "XPTO",
                maxLength = 200)
        @Size(max = 200, message = "Nome fantasia deve ter no máximo {max} caracteres")
        String tradeName,

        @Schema(description = "Código interno único do cliente.", example = "CLI-001",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
        @NotBlank(message = "Código é obrigatório")
        @Size(max = 50, message = "Código deve ter no máximo {max} caracteres")
        String code,

        @Schema(description = "Tipo de pessoa: física (CPF) ou jurídica (CNPJ).",
                example = "JURIDICA", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Tipo de pessoa é obrigatório")
        PersonType personType,

        @Schema(description = "Documento fiscal: CPF (11 dígitos) ou CNPJ (14 dígitos), com ou sem formatação.",
                example = "12.345.678/0001-90", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
        @NotBlank(message = "Documento (CPF/CNPJ) é obrigatório")
        @Size(max = 20, message = "Documento deve ter no máximo {max} caracteres")
        String taxId,

        @Schema(description = "Inscrição Estadual.", example = "123.456.789.012",
                maxLength = 30)
        @Size(max = 30, message = "Inscrição estadual deve ter no máximo {max} caracteres")
        String stateRegistration,

        @Schema(description = "Inscrição Municipal.", example = "9876543",
                maxLength = 30)
        @Size(max = 30, message = "Inscrição municipal deve ter no máximo {max} caracteres")
        String municipalRegistration,

        @Schema(description = "Endereço do cliente.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Endereço é obrigatório")
        @Valid
        AddressDto address,

        @Schema(description = "Status inicial. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ClientStatus status
) {
}
