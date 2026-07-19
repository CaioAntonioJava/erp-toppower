package br.com.toppower.erp_toppower.product.dto;

import br.com.toppower.erp_toppower.product.enums.OrigemProduto;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Atualização parcial do produto (PATCH). Todos os campos são opcionais:
 * envie apenas os campos que deseja alterar.
 */
@Schema(name = "ProductUpdateRequest", description = "Dados para atualização parcial de um produto (PATCH).")
public record ProductUpdateRequest(

        @Schema(description = "Novo nome do produto.", maxLength = 150)
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Novo código do produto (SKU). Opcional — envie apenas se quiser definir/alterar o SKU.",
                maxLength = 50)
        @Size(max = 50, message = "Código deve ter no máximo {max} caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Código aceita apenas letras, números, ponto, underline e hífen")
        String code,

        @Schema(description = "Nova unidade de medida (código SEFAZ usado na NF-e).",
                allowableValues = {
                        "UN", "MTR", "M2", "M3", "KG", "G", "L", "ML",
                        "PC", "CX", "PCT", "RL", "CEM", "PAR", "DZ", "CJ", "FR", "AMP", "BTE"
                })
        UnitType unitType,

        @Schema(description = "Novo status do produto.",
                allowableValues = {"ATIVO", "INATIVO"})
        ProductStatus status,

        @Schema(description = "Novo preco unitário.")
        @Positive(message = "Preço deve ser maior que zero")
        BigDecimal price,

        @Schema(description = "Nova quantidade em estoque.")
        @DecimalMin(value = "0.0", message = "Estoque não pode ser negativo")
        BigDecimal stockQuantity,

        // ----- Campos fiscais (Simples Nacional) — todos opcionais no PATCH -----

        @Schema(description = "Novo NCM (8 dígitos numéricos).")
        @Pattern(regexp = "^\\d{8}$", message = "NCM deve ter exatamente 8 dígitos numéricos")
        String ncm,

        @Schema(description = "Nova origem da mercadoria.",
                allowableValues = {
                        "NACIONAL", "ESTRANGEIRA_IMPORTACAO_DIRETA", "ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO",
                        "NACIONAL_IMPORTACAO_SUPERIOR_40", "NACIONAL_PROCESSOS_PRODUTIVOS_BASICOS",
                        "NACIONAL_IMPORTACAO_SUPERIOR_70", "ESTRANGEIRA_IMPORTACAO_DIRETA_SEM_SIMILAR",
                        "ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO_SEM_SIMILAR", "NACIONAL_IMPORTACAO_ACIMA_70"
                })
        OrigemProduto origem,

        @Schema(description = "Novo código de barras / GTIN (8 a 14 dígitos numéricos).")
        @Pattern(regexp = "^\\d{8,14}$", message = "GTIN deve ter entre 8 e 14 dígitos numéricos")
        String codigoBarras,

        @Schema(description = "Novo CEST (7 dígitos numéricos).")
        @Pattern(regexp = "^\\d{7}$", message = "CEST deve ter exatamente 7 dígitos numéricos")
        String cest,

        @Schema(description = "Novo EX TIPI (2 dígitos numéricos).")
        @Pattern(regexp = "^\\d{2}$", message = "EX TIPI deve ter exatamente 2 dígitos numéricos")
        String exTipi,

        @Schema(description = "Novo peso líquido em kg.")
        @PositiveOrZero(message = "Peso líquido não pode ser negativo")
        BigDecimal pesoLiquido,

        @Schema(description = "Novo peso bruto em kg.")
        @PositiveOrZero(message = "Peso bruto não pode ser negativo")
        BigDecimal pesoBruto,

        @Schema(description = "Novo CSOSN (3 dígitos numéricos).")
        @Pattern(regexp = "^\\d{3}$", message = "CSOSN deve ter exatamente 3 dígitos numéricos")
        String csosn,

        @Schema(description = "Nova alíquota do ICMS-ST (%).")
        @PositiveOrZero(message = "Alíquota do ICMS-ST não pode ser negativa")
        BigDecimal aliquotaIcmsSt,

        @Schema(description = "Nova MVA para ST (%).")
        @PositiveOrZero(message = "MVA não pode ser negativa")
        BigDecimal mvaSt,

        @Schema(description = "Novo CST do IPI (2 dígitos numéricos).")
        @Pattern(regexp = "^\\d{2}$", message = "CST do IPI deve ter exatamente 2 dígitos numéricos")
        String cstIpi,

        @Schema(description = "Nova classe de enquadramento do IPI (5 dígitos numéricos).")
        @Pattern(regexp = "^\\d{5}$", message = "Classe de enquadramento do IPI deve ter 5 dígitos numéricos")
        String classeEnqIpi,

        @Schema(description = "Novo CST do PIS (2 dígitos numéricos).")
        @Pattern(regexp = "^\\d{2}$", message = "CST do PIS deve ter exatamente 2 dígitos numéricos")
        String cstPis,

        @Schema(description = "Novo CST do COFINS (2 dígitos numéricos).")
        @Pattern(regexp = "^\\d{2}$", message = "CST do COFINS deve ter exatamente 2 dígitos numéricos")
        String cstCofins
) {
}