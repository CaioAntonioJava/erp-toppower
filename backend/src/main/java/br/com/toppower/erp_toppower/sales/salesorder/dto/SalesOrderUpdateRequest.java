package br.com.toppower.erp_toppower.sales.salesorder.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Atualização parcial (PATCH) de um pedido de venda.
 *
 * <p>Todos os campos são opcionais: envie apenas os que deseja alterar.
 * Itens são tratados como <b>substituição completa</b>: ao enviar uma
 * nova lista, os itens anteriores são removidos e os novos são criados.</p>
 *
 * <p>O número, a data de emissão, o status, a origem
 * ({@code quotationUuid}/{@code quotationNumber}) <b>não</b> podem ser
 * alterados por este request. Use os endpoints dedicados para
 * transição de status e cancelamento.</p>
 *
 * <p>Pedidos {@code FINALIZADO} ou {@code CANCELADO}
 * não podem ser editados (lança 409).</p>
 */
@Schema(name = "SalesOrderUpdateRequest", description = "Dados para atualização parcial de um pedido (PATCH).")
public record SalesOrderUpdateRequest(

        @Schema(description = "Novo UUID do cliente pessoa física.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID customerUuid,

        @Schema(description = "Novo UUID da empresa (pessoa jurídica).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID companyUuid,

        @Schema(description = "Novo valor para 'Aos cuidados de'.", maxLength = 150,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 150, message = "Aos cuidados de deve ter no máximo {max} caracteres")
        String attention,

        @Schema(description = "Novo UUID do vendedor responsável.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID sellerUuid,

        @Schema(description = "Nova lista de itens (substitui a anterior por completo). "
                + "Se informada, deve conter ao menos um item.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @NotEmpty(message = "A lista de itens não pode ser vazia quando informada")
        @Valid
        List<SalesOrderItemRequest> items,

        @Schema(description = "Novo tipo de desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Novo valor do desconto global.",
                example = "50.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
        BigDecimal discount,

        @Schema(description = "Nova condição de pagamento.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Novas observações livres (enviar string vazia para limpar).",
                example = "Entrega em até 5 dias úteis.", maxLength = 2000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 2000, message = "Observações devem ter no máximo {max} caracteres")
        String notes,

        @Schema(description = "Novo tipo de frete (CIF/FOB).",
                allowableValues = {"CIF", "FOB"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        FreightType freightType,

        @Schema(description = "Novo valor do frete (somado ao total após o desconto).",
                example = "45.90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Frete inválido")
        BigDecimal freightValue,

        @Schema(description = "Nova transportadora (Carrier) responsável pelo frete. "
                + "Envie nulo para remover a transportadora vinculada.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID carrierUuid,

        @Schema(description = "Nova margem de lucro (percentual) aplicada sobre o preço unitário dos itens. "
                + "Opcional — quando omitida, mantém a margem atual. Informação interna, não exibida no PDF.",
                example = "10.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Margem de lucro não pode ser negativa")
        @Digits(integer = 3, fraction = 2, message = "Margem de lucro inválida")
        BigDecimal profitMargin
) {
}