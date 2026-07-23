package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Dados de um item de produto extraído da NF-e, com status de importação.
 */
@Schema(name = "NfeItemData",
        description = "Dados de um item da NF-e com status de importação.")
public record NfeItemData(

        @Schema(description = "Status do produto na importação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        ItemStatus status,

        @Schema(description = "ID do produto, se já cadastrado.")
        Long productId,

        @Schema(description = "Índice do item na nota (0-based). Chave estável "
                + "usada para correlacionar com NfeConfirmItem no confirm.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int itemIndex,

        @Schema(description = "Motivo do match: FORNECEDOR, EAN, CODIGO, NOME "
                + "ou null quando NOVO.")
        String matchReason,

        @Schema(description = "ID do produto candidato sugerido por similaridade "
                + "(apenas quando status = DIVERGENTE).")
        Long candidateProductId,

        @Schema(description = "Nome do produto cadastrado candidato (para "
                + "comparação no DIVERGENTE).")
        String existingProductName,

        @Schema(description = "Código do produto na NF-e (cProd).")
        String code,

        @Schema(description = "Código de barras (cEAN/GTIN).")
        String codigoBarras,

        @Schema(description = "Descrição do produto na NF-e (xProd).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "NCM.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String ncm,

        @Schema(description = "CEST.")
        String cest,

        @Schema(description = "Unidade de medida (uCom).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String unit,

        @Schema(description = "Quantidade (qCom).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantity,

        @Schema(description = "Valor unitário (vUnCom).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal unitValue,

        @Schema(description = "Valor total do item (vProd).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalValue,

        @Schema(description = "Origem do produto (campo orig do ICMS).")
        String origem,

        @Schema(description = "Peso líquido.")
        BigDecimal pesoLiquido,

        @Schema(description = "Peso bruto.")
        BigDecimal pesoBruto
) {
}