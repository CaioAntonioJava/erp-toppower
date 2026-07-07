package br.com.toppower.erp_toppower.sales.salesorder.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.salesorder.enums.SalesOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Representação completa de um pedido de venda retornada pela API,
 * incluindo itens e totais calculados.
 *
 * <p><b>Não expõe margem de lucro</b> — o pedido é o documento externo
 * enviado ao cliente, e a margem é informação interna da
 * {@code Quotation}.</p>
 */
@Schema(name = "SalesOrderResponse", description = "Representação completa de um pedido de venda.")
public record SalesOrderResponse(

        @Schema(description = "Identificador único (UUID) do pedido.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Número sequencial do pedido (sem prefixo).", example = "1000",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long number,

        @Schema(description = "Data de emissão do pedido.", example = "2026-07-02",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate orderDate,

        @Schema(description = "UUID do cliente pessoa física (presente quando o comprador for PF).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID customerUuid,

        @Schema(description = "UUID da empresa (presente quando o comprador for PJ).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID companyUuid,

        @Schema(description = "Tipo de cliente referenciado pelo pedido.",
                allowableValues = {"CUSTOMER", "COMPANY"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ClientType clientType,

        @Schema(description = "Nome de exibição do cliente (PF: nome; PJ: nome fantasia se houver, senão razão social). "
                + "Resolvido no backend a partir do UUID referenciado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientName,

        @Schema(description = "Código interno do cliente (ex.: \"CLI000001\", \"EMP000001\"). "
                + "Resolvido no backend a partir do UUID referenciado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientCode,

        @Schema(description = "Aos cuidados de.", example = "Sr. João Silva",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String attention,

        @Schema(description = "UUID do vendedor.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID sellerUuid,

        @Schema(description = "Nome do vendedor (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String sellerName,

        @Schema(description = "Itens do pedido.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<SalesOrderItemResponse> items,

        @Schema(description = "Tipo de aplicação do desconto global.",
                allowableValues = {"AMOUNT", "PERCENT"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        DiscountType discountType,

        @Schema(description = "Valor do desconto global.", example = "50.00",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal discount,

        @Schema(description = "Condição de pagamento.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Observações livres.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String notes,

        @Schema(description = "Tipo de frete.",
                allowableValues = {"CIF", "FOB"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        FreightType freightType,

        @Schema(description = "Valor do frete (somado ao total após o desconto global).",
                example = "45.90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal freightValue,

        @Schema(description = "Status atual do pedido.",
                allowableValues = {"ABERTO", "FINALIZADO", "CANCELADO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        SalesOrderStatus status,

        @Schema(description = "UUID da proposta que deu origem ao pedido (nulo em criação direta).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID quotationUuid,

        @Schema(description = "Número da proposta que deu origem ao pedido (nulo em criação direta).",
                example = "1500", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long quotationNumber,

        @Schema(description = "Soma dos totais líquidos dos itens (após descontos por item), antes do desconto global.",
                example = "1450.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal subtotal,

        @Schema(description = "Total final do pedido (subtotal menos o desconto global, mais o frete).",
                example = "1440.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal total,

        @Schema(description = "Valor em R$ do desconto global efetivamente aplicado.",
                example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal globalDiscountValue,

        @Schema(description = "Soma das quantidades de todos os itens (unidades comercializadas).",
                example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer totalQuantity,

        @Schema(description = "Data de criação do registro.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String updatedBy
) {

    /**
     * Tipo de cliente referenciado pelo pedido (polimorfismo por
     * duas FKs nullable). Usado para indicar qual campo
     * ({@code customerUuid} ou {@code companyUuid}) está populado.
     */
    public enum ClientType {
        CUSTOMER,
        COMPANY
    }
}