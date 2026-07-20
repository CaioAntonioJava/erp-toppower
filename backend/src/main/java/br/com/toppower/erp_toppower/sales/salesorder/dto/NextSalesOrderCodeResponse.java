package br.com.toppower.erp_toppower.sales.salesorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta do endpoint {@code GET /sales-orders/next-code}. Retorna o
 * código formatado previsto para o próximo pedido de venda (ex.:
 * {@code "PV-2800-2026"}).
 */
@Schema(name = "NextSalesOrderCodeResponse",
        description = "Próximo código de pedido de venda previsto.")
public record NextSalesOrderCodeResponse(

        @Schema(description = "Prefixo do código (ex.: \"PV\").",
                example = "PV", requiredMode = Schema.RequiredMode.REQUIRED)
        String prefix,

        @Schema(description = "Numeral sequencial do código (reseta por ano).",
                example = "2800", requiredMode = Schema.RequiredMode.REQUIRED)
        Long sequence,

        @Schema(description = "Ano corrente, parte final do código.",
                example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer year,

        @Schema(description = "Código formatado completo (ex.: \"PV-2800-2026\").",
                example = "PV-2800-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code
) {
}