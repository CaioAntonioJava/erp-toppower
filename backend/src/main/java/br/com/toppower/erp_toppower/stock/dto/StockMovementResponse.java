package br.com.toppower.erp_toppower.stock.dto;

import br.com.toppower.erp_toppower.stock.enums.MovementSource;
import br.com.toppower.erp_toppower.stock.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Movimentação de estoque retornada pela API. {@code productName} e
 * {@code productCode} são resolvidos no momento da leitura a partir do
 * produto referenciado (não são snapshots persistidos), espelhando o
 * padrão de {@code SalesOrderResponse.clientName}.
 */
@Schema(name = "StockMovementResponse",
        description = "Registro de uma movimentação de estoque (entrada, saída ou estorno).")
public record StockMovementResponse(

        @Schema(description = "Identificador (ID) da movimentação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Identificador (ID) do produto movimentado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long productId,

        @Schema(description = "Nome do produto no momento da consulta (resolvido, não snapshot).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String productName,

        @Schema(description = "SKU do produto no momento da consulta. Nulo quando o produto não tem SKU.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String productCode,

        @Schema(description = "Variação aplicada ao saldo. Positiva para entradas/estornos de saída, "
                + "negativa para saídas/estornos de entrada.",
                example = "-2.000", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantityChange,

        @Schema(description = "Saldo do produto imediatamente antes da movimentação.",
                example = "10.000", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal stockBefore,

        @Schema(description = "Saldo do produto imediatamente depois da movimentação.",
                example = "8.000", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal stockAfter,

        @Schema(description = "Tipo da movimentação.",
                allowableValues = {"ENTRADA", "SAIDA", "ESTORNO_SAIDA", "ESTORNO_ENTRADA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        MovementType type,

        @Schema(description = "Módulo de origem da movimentação.",
                allowableValues = {"SALES_ORDER", "MANUAL"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        MovementSource source,

        @Schema(description = "ID do documento de origem (ex.: SalesOrder.id). "
                + "Nulo quando não há origem rastreável.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long sourceId,

        @Schema(description = "Número legível do documento de origem (ex.: SalesOrder.number).",
                example = "1001", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long sourceNumber,

        @Schema(description = "Observação livre descrevendo o motivo da movimentação.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String reason,

        @Schema(description = "Indica se esta movimentação foi estornada por uma complementar.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean reversed,

        @Schema(description = "Quando esta movimentação é um estorno, ID da movimentação original "
                + "sendo desfeita. Nulo para movimentações primárias.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long reversalOfId,

        @Schema(description = "Data/hora de criação da movimentação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "E-mail do usuário que criou a movimentação.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String createdBy
) {
}