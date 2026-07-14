package br.com.toppower.erp_toppower.customer.dto;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "CustomerResponse", description = "Representação pública de um cliente pessoa física retornada pela API.")
public record CustomerResponse(

        @Schema(description = "Identificador único (ID) do cliente.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Nome completo da pessoa.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "E-mail de contato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Telefone de contato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @Schema(description = "CPF único.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String cpf,

        @Schema(description = "Código interno.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Endereço do cliente.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AddressDto address,

        @Schema(description = "Status atual do cliente.",
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
