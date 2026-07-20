package br.com.toppower.erp_toppower.sales.salesorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Linha de produto retornada pela API, com o total já calculado
 * ({@code unitPrice * quantity}, sendo {@code unitPrice} já majorado
 * pela margem efetiva). O {@code baseUnitPrice} preserva o preço
 * original (sem margem) para que o formulário de edição não reaplique
 * a margem sobre um snapshot já majorado.
 */
@Schema(name = "SalesOrderItemResponse", description = "Linha de produto de um pedido de venda.")
public record SalesOrderItemResponse(

        @Schema(description = "Identificador (UUID) da linha.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Identificador (UUID) do produto referenciado.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long productId,

        @Schema(description = "Quantidade do produto.", example = "2.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantity,

        @Schema(description = "Preço unitário do produto (snapshot, com margem aplicada quando houver).",
                example = "165.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal unitPrice,

        @Schema(description = "Preço unitário original (sem margem). Igual ao unitPrice quando não há margem.",
                example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal baseUnitPrice,

        @Schema(description = "Subtotal bruto da linha (unitPrice * quantity).",
                example = "300.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal lineSubtotal,

        @Schema(description = "Margem de lucro (%) aplicada a esta linha. "
                + "Nula quando a linha usou a margem do cabeçalho do pedido.",
                example = "12.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal profitMargin,

        @Schema(description = "Total da linha (unitPrice * quantity, já com margem embutida). "
                + "Entra no subtotal do pedido.",
                example = "300.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalPrice
) {
}