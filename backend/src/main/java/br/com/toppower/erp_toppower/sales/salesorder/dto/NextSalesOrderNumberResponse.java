package br.com.toppower.erp_toppower.sales.salesorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta do endpoint {@code GET /sales-orders/next-number}. Expõe o
 * próximo número que seria atribuído a um novo pedido, sem persistir.
 */
@Schema(name = "NextSalesOrderNumberResponse",
        description = "Pré-visualização do próximo número de pedido de venda.")
public record NextSalesOrderNumberResponse(

        @Schema(description = "Próximo número sequencial (ex.: 1000, 1001, 1002, ...).",
                example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
        Long number
) {
}