package br.com.toppower.erp_toppower.sales.salesorder.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linha de produto de um pedido de venda. Usado tanto no
 * {@code SalesOrderCreateRequest} quanto no {@code SalesOrderUpdateRequest}.
 *
 * <p>O {@code totalPrice} <b>não</b> é informado pelo cliente — ele é
 * calculado pelo serviço como {@code unitPrice * quantity - discount}
 * (desconto interpretado conforme {@code discountType}).</p>
 */
@Schema(name = "SalesOrderItemRequest", description = "Linha de produto de um pedido de venda.")
public record SalesOrderItemRequest(

        @Schema(description = "Identificador (UUID) do produto a ser incluído na linha.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Produto é obrigatório")
        UUID productUuid,

        @Schema(description = "Quantidade do produto. Suporta até 4 casas decimais (ex.: metros).",
                example = "2.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
        @Digits(integer = 6, fraction = 4, message = "Quantidade inválida")
        BigDecimal quantity,

        @Schema(description = "Preço unitário do produto no momento da emissão (snapshot).",
                example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.00", message = "Preço unitário não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Preço unitário inválido")
        BigDecimal unitPrice,

        @Schema(description = "Tipo de aplicação do desconto da linha (AMOUNT = R$ fixo, PERCENT = %). "
                + "Quando omitido, a linha não tem desconto.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto da linha, interpretado conforme discountType. "
                + "Quando omitido, a linha não tem desconto.",
                example = "10.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
        BigDecimal discount
) {
}