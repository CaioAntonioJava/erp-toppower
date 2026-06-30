package br.com.toppower.erp_toppower.seller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "SellerResponse", description = "Representação pública de um vendedor retornado pela API.")
public record SellerResponse(

        @Schema(description = "Identificador único (UUID) do vendedor.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Nome completo do vendedor.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "E-mail de contato do vendedor.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Telefone de contato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @Schema(description = "CPF único do vendedor.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String cpf,

        @Schema(description = "Percentual de comissão do vendedor (ex: 5.50 = 5,50%).",
                example = "5.50", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal commissionRate,

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
