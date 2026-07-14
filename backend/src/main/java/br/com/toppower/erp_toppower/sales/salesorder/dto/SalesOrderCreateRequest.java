package br.com.toppower.erp_toppower.sales.salesorder.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados para criação direta de um novo pedido de venda (sem proposta
 * de origem).
 *
 * <p>O número do pedido é gerado automaticamente pelo servidor (a
 * partir de {@code 1000}). A data de emissão é preenchida com a data
 * atual no momento da persistência. O status inicial é
 * {@code ABERTO}.</p>
 *
 * <p>Deve ser informado exatamente <b>um</b> entre {@link #customerUuid}
 * (cliente pessoa física) e {@link #companyUuid} (cliente pessoa
 * jurídica). A validação dessa invariante é feita no serviço.</p>
 *
 * <p>A margem de lucro ({@link #profitMargin}) é <b>opcional</b>: quando
 * informada, é aplicada item a item sobre o preço unitário, embutindo-se
 * no {@code unitPrice} e no total do pedido. Quando omitida, nenhum
 * acréscimo é aplicado. A margem é informação interna de precificação e
 * <b>não</b> aparece no PDF do pedido.</p>
 */
@Schema(name = "SalesOrderCreateRequest", description = "Dados para cadastro direto de um novo pedido de venda.")
public record SalesOrderCreateRequest(

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

        @Schema(description = "UUID do vendedor responsável pelo pedido.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Vendedor é obrigatório")
        Long sellerId,

        @Schema(description = "Itens do pedido. O pedido deve ter ao menos um item.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "O pedido deve ter ao menos um item")
        @Valid
        List<SalesOrderItemRequest> items,

        @Schema(description = "Tipo de aplicação do desconto global (AMOUNT = R$ fixo, PERCENT = %). "
                + "Quando omitido, o pedido não tem desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto global, interpretado conforme discountType.",
                example = "50.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
        BigDecimal discount,

        @Schema(description = "Condição de pagamento acordada com o comprador.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Observações livres do pedido (instruções de entrega, garantias, etc.).",
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

        @Schema(description = "UUID da transportadora (Carrier) responsável pelo frete. Opcional.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long carrierId,

        @Schema(description = "Margem de lucro (percentual) aplicada sobre o preço unitário dos itens. "
                + "Opcional — quando omitida, nenhum acréscimo é aplicado. Informação interna, "
                + "não exibida no PDF do pedido.",
                example = "10.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Margem de lucro não pode ser negativa")
        @Digits(integer = 3, fraction = 2, message = "Margem de lucro inválida")
        BigDecimal profitMargin
) {
}