package br.com.toppower.erp_toppower.sales.quotation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado da simulação de totais de uma proposta comercial, sem
 * persistir nada. Retornada pelo endpoint {@code POST /quotations/simulate}
 * para que o frontend possa exibir um preview em tempo real dos totais
 * calculados pelo backend.
 */
@Schema(name = "QuotationSimulateResponse",
        description = "Totais calculados de uma proposta comercial sem persistência (preview).")
public record QuotationSimulateResponse(

        @Schema(description = "Itens da proposta com os totais líquidos calculados.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<QuotationItemResponse> items,

        @Schema(description = "Soma dos totais líquidos dos itens (após descontos por item), antes do desconto global.",
                example = "1450.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal subtotal,

        @Schema(description = "Valor em R$ do desconto global efetivamente aplicado (já considerando a margem de lucro sobre o subtotal).",
                example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal globalDiscountValue,

        @Schema(description = "Total final da proposta (subtotal com margem de lucro aplicada, menos o desconto global, mais o frete).",
                example = "1590.49", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal total,

        @Schema(description = "Soma das quantidades de todos os itens (unidades comercializadas).",
                example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer totalQuantity
) {
}