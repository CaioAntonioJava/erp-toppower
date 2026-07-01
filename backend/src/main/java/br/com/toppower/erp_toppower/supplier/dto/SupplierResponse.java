package br.com.toppower.erp_toppower.supplier.dto;

import br.com.toppower.erp_toppower.supplier.enums.SupplierStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "SupplierResponse", description = "Representação pública de um fornecedor retornado pela API.")
public record SupplierResponse(

        @Schema(description = "Identificador único (UUID) do fornecedor.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Razão social.", requiredMode = Schema.RequiredMode.REQUIRED)
        String legalName,

        @Schema(description = "Nome fantasia.")
        String tradeName,

        @Schema(description = "CNPJ.", requiredMode = Schema.RequiredMode.REQUIRED)
        String taxId,

        @Schema(description = "Inscrição Estadual.")
        String stateRegistration,

        @Schema(description = "Inscrição Municipal.")
        String municipalRegistration,

        @Schema(description = "E-mail de contato.", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Telefone de contato.")
        String phone,

        @Schema(description = "Nome da pessoa de contato.")
        String contactName,

        @Schema(description = "Endereço do fornecedor.", requiredMode = Schema.RequiredMode.REQUIRED)
        AddressDto address,

        @Schema(description = "Status atual.", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        SupplierStatus status,

        @Schema(description = "Data de criação.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.")
        String updatedBy
) {
}
