package br.com.toppower.erp_toppower.product.dto;

import br.com.toppower.erp_toppower.product.enums.OrigemProduto;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "ProductResponse", description = "Representação pública de um produto retornado pela API.")
public record ProductResponse(

        @Schema(description = "Identificador único (ID) do produto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Nome do produto.", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Código único (SKU) do produto.", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Unidade de medida.",
                allowableValues = {"UNIDADE", "METROS", "BOBINA", "PECAS", "QUILOS", "ROLO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        UnitType unitType,

        @Schema(description = "Status atual.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ProductStatus status,

        @Schema(description = "Preço unitário de venda.", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal price,

        @Schema(description = "Quantidade em estoque.", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal stockQuantity,

        // ----- Campos fiscais (Simples Nacional) -----

        @Schema(description = "NCM — Nomenclatura Comum do Mercosul (8 dígitos). Pode ser nulo em produtos legados.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String ncm,

        @Schema(description = "Origem da mercadoria (campo orig da NF-e).",
                allowableValues = {
                        "NACIONAL", "ESTRANGEIRA_IMPORTACAO_DIRETA", "ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO",
                        "NACIONAL_IMPORTACAO_SUPERIOR_40", "NACIONAL_PROCESSOS_PRODUTIVOS_BASICOS",
                        "NACIONAL_IMPORTACAO_SUPERIOR_70", "ESTRANGEIRA_IMPORTACAO_DIRETA_SEM_SIMILAR",
                        "ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO_SEM_SIMILAR", "NACIONAL_IMPORTACAO_ACIMA_70"
                },
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        OrigemProduto origem,

        @Schema(description = "Código de barras / GTIN.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String codigoBarras,

        @Schema(description = "CEST — Código Especificador da Substituição Tributária.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String cest,

        @Schema(description = "EX TIPI — Exceção da TIPI.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String exTipi,

        @Schema(description = "Peso líquido em kg.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal pesoLiquido,

        @Schema(description = "Peso bruto em kg.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal pesoBruto,

        @Schema(description = "CSOSN — Código de Situação da Operação no Simples Nacional.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String csosn,

        @Schema(description = "Alíquota do ICMS-ST (%).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal aliquotaIcmsSt,

        @Schema(description = "MVA para ST (%).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal mvaSt,

        @Schema(description = "CST do IPI.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String cstIpi,

        @Schema(description = "Classe de enquadramento do IPI.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String classeEnqIpi,

        @Schema(description = "CST do PIS.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String cstPis,

        @Schema(description = "CST do COFINS.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String cstCofins,

        // ----- Auditoria -----

        @Schema(description = "Data de criação.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da ultima atualização.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a ultima atualização.")
        String updatedBy
) {
}