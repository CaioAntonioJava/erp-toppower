package br.com.toppower.erp_toppower.product.dto;

import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "ProductResponse", description = "Representacao publica de um produto retornado pela API.")
public record ProductResponse(

        @Schema(description = "Identificador unico (UUID) do produto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Nome do produto.", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Codigo unico (SKU) do produto.", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Unidade de medida.",
                allowableValues = {"PECAS", "METROS", "BOBINA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        UnitType unitType,

        @Schema(description = "Status atual.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ProductStatus status,

        @Schema(description = "Preco unitario de venda.", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal price,

        @Schema(description = "Quantidade em estoque.", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal stockQuantity,

        @Schema(description = "Data de criacao.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da ultima atualizacao.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuario que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuario que fez a ultima atualizacao.")
        String updatedBy
) {
}
