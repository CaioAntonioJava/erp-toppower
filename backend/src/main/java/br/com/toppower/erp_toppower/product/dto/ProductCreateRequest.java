package br.com.toppower.erp_toppower.product.dto;

import br.com.toppower.erp_toppower.product.enums.OrigemProduto;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(name = "ProductCreateRequest", description = "Dados para cadastro de um novo produto.")
public record ProductCreateRequest(

        @Schema(description = "Nome do produto.", example = "Cabo Flexivel 2,5mm",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Código único do produto (SKU). Opcional — se omitido, o produto é cadastrado sem SKU.",
                example = "CB-FLEX-2.5",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 50)
        @Size(max = 50, message = "Código deve ter no máximo {max} caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Código aceita apenas letras, números, ponto, underline e hífen")
        String code,

        @Schema(description = "Unidade de medida em que o produto é comercializado.",
                example = "METROS",
                allowableValues = {"UNIDADE", "METROS", "BOBINA", "PECAS", "QUILOS", "ROLO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Unidade de medida é obrigatória")
        UnitType unitType,

        @Schema(description = "Status inicial do produto. Se omitido, assume ATIVO.",
                example = "ATIVO",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ProductStatus status,

        @Schema(description = "Preço unitário de venda.",
                example = "2.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Preço é obrigatório")
        @Positive(message = "Preço deve ser maior que zero")
        BigDecimal price,

        @Schema(description = "Quantidade em estoque (permite fracionamento para METROS/BOBINA).",
                example = "100.0000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantidade em estoque é obrigatória")
        @DecimalMin(value = "0.0", message = "Estoque não pode ser negativo")
        BigDecimal stockQuantity,

        // ----- Campos fiscais (Simples Nacional) -----

        @Schema(description = "NCM — Nomenclatura Comum do Mercosul (8 dígitos). Obrigatório na NF-e.",
                example = "8544.42.00 — use apenas dígitos: 85444200",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "NCM é obrigatório")
        @Pattern(regexp = "^\\d{8}$", message = "NCM deve ter exatamente 8 dígitos numéricos")
        String ncm,

        @Schema(description = "Origem da mercadoria (campo orig da NF-e). Default NACIONAL.",
                example = "NACIONAL",
                allowableValues = {
                        "NACIONAL", "ESTRANGEIRA_IMPORTACAO_DIRETA", "ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO",
                        "NACIONAL_IMPORTACAO_SUPERIOR_40", "NACIONAL_PROCESSOS_PRODUTIVOS_BASICOS",
                        "NACIONAL_IMPORTACAO_SUPERIOR_70", "ESTRANGEIRA_IMPORTACAO_DIRETA_SEM_SIMILAR",
                        "ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO_SEM_SIMILAR", "NACIONAL_IMPORTACAO_ACIMA_70"
                },
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        OrigemProduto origem,

        @Schema(description = "Código de barras / GTIN (EAN-13/14). Opcional.",
                example = "7891234567890", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{8,14}$", message = "GTIN deve ter entre 8 e 14 dígitos numéricos")
        String codigoBarras,

        @Schema(description = "CEST — Código Especificador da Substituição Tributária (7 dígitos). Opcional.",
                example = "17.031.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{7}$", message = "CEST deve ter exatamente 7 dígitos numéricos")
        String cest,

        @Schema(description = "EX TIPI — Exceção da TIPI (2 dígitos). Opcional.",
                example = "00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{2}$", message = "EX TIPI deve ter exatamente 2 dígitos numéricos")
        String exTipi,

        @Schema(description = "Peso líquido em kg. Opcional.",
                example = "0.500", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @PositiveOrZero(message = "Peso líquido não pode ser negativo")
        BigDecimal pesoLiquido,

        @Schema(description = "Peso bruto em kg. Opcional.",
                example = "0.550", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @PositiveOrZero(message = "Peso bruto não pode ser negativo")
        BigDecimal pesoBruto,

        @Schema(description = "CSOSN — Código de Situação da Operação no Simples Nacional. Default 102.",
                example = "102",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{3}$", message = "CSOSN deve ter exatamente 3 dígitos numéricos")
        String csosn,

        @Schema(description = "Alíquota do ICMS-ST (%) — apenas para produtos sujeitos à ST. Opcional.",
                example = "18.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @PositiveOrZero(message = "Alíquota do ICMS-ST não pode ser negativa")
        BigDecimal aliquotaIcmsSt,

        @Schema(description = "MVA (Margem de Valor Adicionado) para ST (%). Opcional.",
                example = "35.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @PositiveOrZero(message = "MVA não pode ser negativa")
        BigDecimal mvaSt,

        @Schema(description = "CST do IPI — no Simples Nacional usa-se 99. Default 99.",
                example = "99", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{2}$", message = "CST do IPI deve ter exatamente 2 dígitos numéricos")
        String cstIpi,

        @Schema(description = "Classe de enquadramento do IPI (5 dígitos). Opcional.",
                example = "00100", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{5}$", message = "Classe de enquadramento do IPI deve ter 5 dígitos numéricos")
        String classeEnqIpi,

        @Schema(description = "CST do PIS — no Simples Nacional usa-se 49. Default 49.",
                example = "49", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{2}$", message = "CST do PIS deve ter exatamente 2 dígitos numéricos")
        String cstPis,

        @Schema(description = "CST do COFINS — no Simples Nacional usa-se 49. Default 49.",
                example = "49", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{2}$", message = "CST do COFINS deve ter exatamente 2 dígitos numéricos")
        String cstCofins
) {
}