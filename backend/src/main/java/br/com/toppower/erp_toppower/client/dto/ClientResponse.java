package br.com.toppower.erp_toppower.client.dto;

import br.com.toppower.erp_toppower.client.enums.ClientStatus;
import br.com.toppower.erp_toppower.client.enums.PersonType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "ClientResponse", description = "Representação pública de um cliente retornado pela API.")
public record ClientResponse(

        @Schema(description = "Identificador único (UUID) do cliente.",
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

        @Schema(description = "Tipo de pessoa: FISICA (CPF) ou JURIDICA (CNPJ).",
                allowableValues = {"FISICA", "JURIDICA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        PersonType personType,

        @Schema(description = "Documento fiscal (CPF ou CNPJ).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String taxId,

        @Schema(description = "Inscrição Estadual.")
        String stateRegistration,

        @Schema(description = "Inscrição Municipal.")
        String municipalRegistration,

        @Schema(description = "Endereço do cliente.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AddressDto address,

        @Schema(description = "Status atual do cliente.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ClientStatus status,

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
