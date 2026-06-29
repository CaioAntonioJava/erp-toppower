package br.com.toppower.erp_toppower.product.dto;

import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "ProductResponse", description = "Representacao publica de um produto retornado pela API.")
public record ProductResponse(

        @Schema(description = "Identificador único (UUID) do produto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Nome do produto.", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Código único (SKU) do produto.", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Unidade de medida.",
                allowableValues = {"UNIDADE", "METROS", "BOBINA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        UnitType unitType,

        @Schema(description = "Status atual.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ProductStatus status,

        @Schema(description = "Preço unitário de venda.", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal price,

        @Schema(description = "Quantidade em estoque.", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal stockQuantity,

        @Schema(description = "Data de criação.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da ultima atualização.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a ultima atualização.")
        String updatedBy
) {
}
