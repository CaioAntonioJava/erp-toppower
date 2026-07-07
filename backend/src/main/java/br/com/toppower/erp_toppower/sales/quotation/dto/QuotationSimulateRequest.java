package br.com.toppower.erp_toppower.sales.quotation.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Dados para simulação dos totais de uma proposta comercial
 * ({@code POST /quotations/simulate}).
 *
 * <p>Variação permissiva de {@link QuotationCreateRequest}: nenhum
 * campo é obrigatório, pois o preview em tempo real pode ser disparado
 * com o formulário ainda incompleto (ex.: sem cliente ou vendedor
 * selecionados). Apenas os campos numéricos mantêm validação de
 * faixa/precisão; o service trata nulos como zero ao calcular.</p>
 *
 * <p>Não persiste nada — é pura para cálculo de preview.</p>
 */
@Schema(name = "QuotationSimulateRequest",
        description = "Dados para simulação de totais de uma proposta comercial (preview, sem persistência).")
public record QuotationSimulateRequest(

        @Schema(description = "UUID do cliente pessoa física (opcional no preview).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID customerUuid,

        @Schema(description = "UUID da empresa (opcional no preview).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID companyUuid,

        @Schema(description = "Aos cuidados de.", maxLength = 150,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 150, message = "Aos cuidados de deve ter no máximo {max} caracteres")
        String attention,

        @Schema(description = "UUID do vendedor (opcional no preview).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID sellerUuid,

        @Schema(description = "Itens da proposta. Itens em estado intermediário são tolerados.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<QuotationSimulateItemRequest> items,

        @Schema(description = "Tipo de aplicação do desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto global, interpretado conforme discountType.",
                example = "50.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
        BigDecimal discount,

        @Schema(description = "Prazo de validade em dias.",
                example = "15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer validityDays,

        @Schema(description = "Condição de pagamento.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Observações livres.", maxLength = 2000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 2000, message = "Observações devem ter no máximo {max} caracteres")
        String notes,

        @Schema(description = "Tipo de frete.",
                allowableValues = {"CIF", "FOB"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        FreightType freightType,

        @Schema(description = "Valor do frete. Somado ao total após o desconto global.",
                example = "45.90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Frete inválido")
        BigDecimal freightValue,

        @Schema(description = "Margem de lucro aplicada sobre o subtotal dos itens (em %).",
                example = "10.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Margem de lucro não pode ser negativa")
        @Digits(integer = 3, fraction = 2, message = "Margem de lucro inválida")
        BigDecimal profitMargin
) {
}