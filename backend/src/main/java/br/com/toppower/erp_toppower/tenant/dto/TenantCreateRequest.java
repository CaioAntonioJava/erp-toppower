package br.com.toppower.erp_toppower.tenant.dto;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.common.validation.ValidCnpj;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "TenantCreateRequest",
        description = "Dados para cadastro de um novo tenant (empresa operadora do ERP). "
                + "O código interno (TEN000001, TEN000002, ...) é gerado automaticamente pelo servidor.")
public record TenantCreateRequest(

        @Schema(description = "Razão social (nome oficial/registrado).",
                example = "TOPPOWER ENGENHARIA LTDA", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 200, message = "Razão social deve ter no máximo {max} caracteres")
        String legalName,

        @Schema(description = "Nome fantasia (nome comercial).", example = "TOPPOWER",
                maxLength = 200)
        @Size(max = 200, message = "Nome fantasia deve ter no máximo {max} caracteres")
        String tradeName,

        @Schema(description = "CNPJ (14 dígitos, com ou sem formatação).",
                example = "12.345.678/0001-90", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
        @NotBlank(message = "CNPJ é obrigatório")
        @ValidCnpj(message = "CNPJ inválido")
        @Size(max = 20, message = "CNPJ deve ter no máximo {max} caracteres")
        String cnpj,

        @Schema(description = "Inscrição Estadual.", example = "123.456.789.012",
                maxLength = 30)
        @Size(max = 30, message = "Inscrição estadual deve ter no máximo {max} caracteres")
        String stateRegistration,

        @Schema(description = "Indica se a empresa é ISENTA de Inscrição Estadual (IE Isento). "
                + "Default: false.", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean stateRegistrationExempt,

        @Schema(description = "Inscrição Municipal.", example = "9876543",
                maxLength = 30)
        @Size(max = 30, message = "Inscrição municipal deve ter no máximo {max} caracteres")
        String municipalRegistration,

        @Schema(description = "Endereço da empresa.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Endereço é obrigatório")
        @Valid
        AddressDto address,

        @Schema(description = "Status inicial. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        RegistrationStatus status
) {
}