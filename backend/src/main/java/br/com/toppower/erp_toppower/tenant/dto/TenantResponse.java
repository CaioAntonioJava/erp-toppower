package br.com.toppower.erp_toppower.tenant.dto;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "TenantResponse", description = "Representação pública de um tenant (empresa operadora) retornada pela API.")
public record TenantResponse(

        @Schema(description = "Identificador único (UUID) do tenant.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Razão social.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String legalName,

        @Schema(description = "Nome fantasia.")
        String tradeName,

        @Schema(description = "Código interno.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "CNPJ (14 dígitos, com ou sem formatação).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String cnpj,

        @Schema(description = "Inscrição Estadual.")
        String stateRegistration,

        @Schema(description = "Indica se a empresa é ISENTA de Inscrição Estadual (IE Isento).",
                allowableValues = {"true", "false"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean stateRegistrationExempt,

        @Schema(description = "Inscrição Municipal.")
        String municipalRegistration,

        @Schema(description = "Endereço da empresa.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AddressDto address,

        @Schema(description = "Status atual do tenant.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        RegistrationStatus status,

        @Schema(description = "Data de criação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.")
        String updatedBy
) {
}