package br.com.toppower.erp_toppower.product.dto;

import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Atualização parcial do produto (PATCH). Todos os campos são opcionais:
 * envie apenas os campos que deseja alterar.
 */
@Schema(name = "ProductUpdateRequest", description = "Dados para atualização parcial de um produto (PATCH).")
public record ProductUpdateRequest(

        @Schema(description = "Novo nome do produto.", maxLength = 150)
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Novo código do produto (SKU). Opcional — envie apenas se quiser definir/alterar o SKU.",
                maxLength = 50)
        @Size(max = 50, message = "Código deve ter no máximo {max} caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Código aceita apenas letras, números, ponto, underline e hífen")
        String code,

        @Schema(description = "Nova unidade de medida.",
                allowableValues = {"UNIDADE", "METROS", "BOBINA"})
        UnitType unitType,

        @Schema(description = "Novo status do produto.",
                allowableValues = {"ATIVO", "INATIVO"})
        ProductStatus status,

        @Schema(description = "Novo preco unitário.")
        @Positive(message = "Preço deve ser maior que zero")
        BigDecimal price,

        @Schema(description = "Nova quantidade em estoque.")
        @DecimalMin(value = "0.0", message = "Estoque não pode ser negativo")
        BigDecimal stockQuantity
) {
}
