package br.com.toppower.erp_toppower.sales.salesorder.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.salesorder.enums.SalesOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação resumida de um pedido de venda, usada em listagens
 * paginadas. Não inclui a lista de itens (carregada sob demanda ao
 * abrir o detalhe).
 */
@Schema(name = "SalesOrderSummaryResponse", description = "Resumo de um pedido de venda para listagens.")
public record SalesOrderSummaryResponse(

        @Schema(description = "Identificador único (UUID) do pedido.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Prefixo do código do pedido (ex.: \"PV\").", example = "PV",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String prefix,

        @Schema(description = "Numeral sequencial do código (reseta por ano).", example = "2800",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long sequence,

        @Schema(description = "Ano de emissão, parte final do código.", example = "2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer year,

        @Schema(description = "Código formatado completo (ex.: \"PV-2800-2026\").",
                example = "PV-2800-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Data de emissão.", example = "2026-07-02",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate orderDate,

        @Schema(description = "Tipo do cliente (pessoa física ou jurídica).",
                allowableValues = {"CUSTOMER", "COMPANY"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        SalesOrderResponse.ClientType clientType,

        @Schema(description = "UUID do cliente referenciado (PF ou PJ).", requiredMode = Schema.RequiredMode.REQUIRED)
        Long clientId,

        @Schema(description = "Nome do cliente (PF ou nome fantasia/razão social da empresa).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String clientName,

        @Schema(description = "Código interno do cliente (ex.: \"CLI000001\", \"EMP000001\"). "
                + "Resolvido no backend a partir do UUID referenciado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientCode,

        @Schema(description = "UUID do vendedor.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long sellerId,

        @Schema(description = "Nome do vendedor (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String sellerName,

        @Schema(description = "Status atual.",
                allowableValues = {"ABERTO", "FINALIZADO", "CANCELADO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        SalesOrderStatus status,

        @Schema(description = "Total de itens (unidades).", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer totalQuantity,

        @Schema(description = "Total final do pedido (após descontos).", example = "1400.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal total,

        @Schema(description = "Condição de pagamento.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Número da proposta de origem (nulo em criação direta).",
                example = "1500", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long quotationNumber
) {
}