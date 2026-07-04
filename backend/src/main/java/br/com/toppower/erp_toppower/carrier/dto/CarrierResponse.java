package br.com.toppower.erp_toppower.carrier.dto;

import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "CarrierResponse", description = "Representação pública de uma transportadora retornada pela API.")
public record CarrierResponse(

        @Schema(description = "Identificador único (UUID) da transportadora.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Nome da transportadora (sempre MAIÚSCULAS).")
        String name,

        @Schema(description = "Valor padrão do frete.")
        BigDecimal freightValue,

        @Schema(description = "Status atual da transportadora.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        CarrierStatus status,

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
