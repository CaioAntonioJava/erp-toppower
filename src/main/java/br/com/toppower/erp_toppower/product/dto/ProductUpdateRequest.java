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
 * Atualizacao parcial do produto (PATCH). Todos os campos sao opcionais:
 * envie apenas os campos que deseja alterar.
 */
@Schema(name = "ProductUpdateRequest", description = "Dados para atualizacao parcial de um produto (PATCH).")
public record ProductUpdateRequest(

        @Schema(description = "Novo nome do produto.", maxLength = 150)
        @Size(max = 150, message = "Nome deve ter no maximo {max} caracteres")
        String name,

        @Schema(description = "Novo codigo do produto (SKU).", maxLength = 50)
        @Size(max = 50, message = "Codigo deve ter no maximo {max} caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Codigo aceita apenas letras, numeros, ponto, underline e hifen")
        String code,

        @Schema(description = "Nova unidade de medida.",
                allowableValues = {"PECAS", "METROS", "BOBINA"})
        UnitType unitType,

        @Schema(description = "Novo status do produto.",
                allowableValues = {"ATIVO", "INATIVO"})
        ProductStatus status,

        @Schema(description = "Novo preco unitario.")
        @Positive(message = "Preco deve ser maior que zero")
        BigDecimal price,

        @Schema(description = "Nova quantidade em estoque.")
        @DecimalMin(value = "0.0", message = "Estoque nao pode ser negativo")
        BigDecimal stockQuantity
) {
}
