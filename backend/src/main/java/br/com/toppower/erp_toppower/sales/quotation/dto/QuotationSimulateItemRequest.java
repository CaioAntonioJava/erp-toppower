package br.com.toppower.erp_toppower.sales.quotation.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

/**
 * Linha de produto enviada à simulação de totais
 * ({@code POST /quotations/simulate}).
 *
 * <p>Variação permissiva de {@link QuotationItemRequest}: os campos
 * numéricos continuam validados quanto a faixa/precisão, mas nenhum é
 * obrigatório — linhas em estado intermediário (ex.: sem produto
 * selecionado) são toleradas e tratadas como zero pelo cálculo. Isso
 * permite que o frontend dispare o preview em tempo real mesmo antes
 * do formulário estar completo.</p>
 */
@Schema(name = "QuotationSimulateItemRequest",
        description = "Linha de produto enviada à simulação de totais. Campos opcionais para tolerar preview parcial.")
public record QuotationSimulateItemRequest(

        @Schema(description = "ID do produto (opcional durante o preview).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long productId,

        @Schema(description = "Quantidade do produto. Suporta até 4 casas decimais.",
                example = "2.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
        @Digits(integer = 6, fraction = 4, message = "Quantidade inválida")
        BigDecimal quantity,

        @Schema(description = "Preço unitário do produto (snapshot).",
                example = "150.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Preço unitário não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Preço unitário inválido")
        BigDecimal unitPrice,

        @Schema(description = "Tipo de aplicação do desconto da linha (AMOUNT = R$ fixo, PERCENT = %).",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto da linha, interpretado conforme discountType.",
                example = "10.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
        BigDecimal discount
) {
}