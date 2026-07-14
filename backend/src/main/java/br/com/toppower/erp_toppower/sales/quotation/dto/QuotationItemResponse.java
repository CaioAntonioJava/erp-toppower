package br.com.toppower.erp_toppower.sales.quotation.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Linha de produto retornada pela API, com o total líquido já calculado
 * ({@code unitPrice * quantity - discount}).
 */
@Schema(name = "QuotationItemResponse", description = "Linha de produto de uma proposta comercial.")
public record QuotationItemResponse(

        @Schema(description = "Identificador (UUID) da linha.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Identificador (UUID) do produto referenciado.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long productId,

        @Schema(description = "Quantidade do produto.", example = "2.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantity,

        @Schema(description = "Preço unitário final do produto (snapshot, com margem de lucro embutida).",
                example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal unitPrice,

        @Schema(description = "Preço unitário original enviado pelo usuário (sem margem de lucro). "
                + "É o valor que o formulário usa como ponto de partida na edição — "
                + "aplicar a margem sobre o `unitPrice` causaria duplicação.",
                example = "130.43", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal baseUnitPrice,

        @Schema(description = "Subtotal bruto da linha (unitPrice * quantity), antes do desconto da linha.",
                example = "300.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal lineSubtotal,

        @Schema(description = "Tipo de aplicação do desconto da linha.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto da linha (R$ ou %).",
                example = "10.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal discount,

        @Schema(description = "Total líquido da linha (subtotal - desconto da linha). Entra no subtotal da proposta.",
                example = "290.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalPrice
) {
}
