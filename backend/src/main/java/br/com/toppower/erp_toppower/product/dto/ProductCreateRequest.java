package br.com.toppower.erp_toppower.product.dto;

import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(name = "ProductCreateRequest", description = "Dados para cadastro de um novo produto.")
public record ProductCreateRequest(

        @Schema(description = "Nome do produto.", example = "Cabo Flexivel 2,5mm",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Código único do produto (SKU). Opcional — se omitido, o produto é cadastrado sem SKU.",
                example = "CB-FLEX-2.5",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 50)
        @Size(max = 50, message = "Código deve ter no máximo {max} caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Código aceita apenas letras, números, ponto, underline e hífen")
        String code,

        @Schema(description = "Unidade de medida em que o produto é comercializado.",
                example = "METROS",
                allowableValues = {"UNIDADE", "METROS", "BOBINA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Unidade de medida é obrigatória")
        UnitType unitType,

        @Schema(description = "Status inicial do produto. Se omitido, assume ATIVO.",
                example = "ATIVO",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ProductStatus status,

        @Schema(description = "Preço unitário de venda.",
                example = "2.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Preço é obrigatório")
        @Positive(message = "Preço deve ser maior que zero")
        BigDecimal price,

        @Schema(description = "Quantidade em estoque (permite fracionamento para METROS/BOBINA).",
                example = "100.0000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantidade em estoque é obrigatória")
        @DecimalMin(value = "0.0", message = "Estoque não pode ser negativo")
        BigDecimal stockQuantity
) {
}
