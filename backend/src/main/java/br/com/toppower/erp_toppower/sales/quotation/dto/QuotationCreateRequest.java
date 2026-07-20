package br.com.toppower.erp_toppower.sales.quotation.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados para criação de uma nova proposta comercial.
 *
 * <p>O número da proposta é gerado automaticamente pelo servidor
 * (a partir de {@code QUO001500}). A data de emissão é preenchida
 * com a data atual no momento da persistência.</p>
 *
 * <p>Deve ser informado exatamente <b>um</b> entre {@link #customerUuid}
 * (cliente pessoa física) e {@link #companyUuid} (cliente pessoa
 * jurídica). A validação dessa invariante é feita no serviço.</p>
 */
@Schema(name = "QuotationCreateRequest", description = "Dados para cadastro de uma nova proposta comercial.")
public record QuotationCreateRequest(

        @Schema(description = "UUID do cliente pessoa física. OBRIGATÓRIO se companyUuid não for informado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long customerId,

        @Schema(description = "UUID da empresa (pessoa jurídica). OBRIGATÓRIO se customerUuid não for informado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long companyId,

        @Schema(description = "Aos cuidados de: nome da pessoa de contato no lado do comprador.",
                example = "Sr. João Silva", maxLength = 150,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 150, message = "Aos cuidados de deve ter no máximo {max} caracteres")
        String attention,

        @Schema(description = "UUID do vendedor responsável pela proposta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Vendedor é obrigatório")
        Long sellerId,

        @Schema(description = "Itens da proposta. A proposta deve ter ao menos um item.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "A proposta deve ter ao menos um item")
        @Valid
        List<QuotationItemRequest> items,

        @Schema(description = "Tipo de aplicação do desconto global (AMOUNT = R$ fixo, PERCENT = %). "
                + "Quando omitido, a proposta não tem desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto global, interpretado conforme discountType.",
                example = "50.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
        BigDecimal discount,

        @Schema(description = "Prazo de validade da proposta em dias, contado a partir da data de emissão.",
                example = "15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Min(value = 1, message = "Validade deve ser de ao menos 1 dia")
        Integer validityDays,

        @Schema(description = "Condição de pagamento acordada com o comprador.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Observações livres da proposta (instruções de entrega, garantias, etc.).",
                example = "Entrega em até 5 dias úteis.", maxLength = 2000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 2000, message = "Observações devem ter no máximo {max} caracteres")
        String notes,

        @Schema(description = "Tipo de frete (CIF = por conta do remetente, FOB = por conta do destinatário).",
                allowableValues = {"CIF", "FOB"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        FreightType freightType,

        @Schema(description = "Valor do frete informado manualmente. Somado ao total após o desconto global.",
                example = "45.90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Frete inválido")
        BigDecimal freightValue,

        @Schema(description = "Margem de lucro (%) aplicada a todos os itens sem margem própria. "
                + "Ex.: 10.00 = 10% aplicado como multiplicação sobre o preço unitário. "
                + "Opcional quando todos os itens informam margem própria.",
                example = "10.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Margem de lucro não pode ser negativa")
        @Digits(integer = 3, fraction = 2, message = "Margem de lucro inválida")
        BigDecimal profitMargin,

        @Schema(description = "UUID da transportadora (Carrier) responsável pelo frete. Opcional.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long carrierId
) {
}
