package br.com.toppower.erp_toppower.product.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.product.enums.OrigemProduto;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends OrganizationScopedEntity {

    @UpperCase
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Código único do produto (SKU). Opcional — quando não informado, o produto
     * é cadastrado sem SKU e a coluna aceita {@code NULL} (constraint única
     * do banco ignora nulos).
     */
    @Column(name = "code", unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 20)
    private UnitType unitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false, precision = 10, scale = 4)
    private BigDecimal stockQuantity;

    // =========================================================================
    // Campos fiscais — Simples Nacional (NF-e)
    // O sistema não emite NF-e, mas os campos são mantidos no cadastro para
    // futura integração com emissores fiscais (NFe.io, Webmania, etc.) e para
    // exportação contábil. Os defaults abaixo refletem o regime do Simples
    // Nacional e são aplicados em {@link #onPrePersist()}.
    // =========================================================================

    /**
     * NCM — Nomenclatura Comum do Mercosul (8 dígitos). Obrigatório na NF-e.
     * Coluna aceita {@code NULL} apenas para preservar produtos legados; a
     * obrigatoriedade é garantida no DTO de criação ({@code @NotBlank}).
     */
    @UpperCase
    @Column(name = "ncm", length = 8)
    private String ncm;

    /**
     * Origem da mercadoria (campo {@code orig} da NF-e). Default {@code NACIONAL}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "origem", length = 30)
    private OrigemProduto origem;

    /**
     * Código de barras / GTIN (EAN-13/14). Opcional — quando ausente, a NF-e
     * informa o literal "SEM GTIN".
     */
    @UpperCase
    @Column(name = "codigo_barras", length = 14)
    private String codigoBarras;

    /**
     * CEST — Código Especificador da Substituição Tributária (7 dígitos).
     * Opcional — exigido apenas para produtos sujeitos a ST.
     */
    @UpperCase
    @Column(name = "cest", length = 7)
    private String cest;

    /**
     * EX TIPI — Exceção da TIPI (2 dígitos). Raro; {@code null}/0 para a maioria.
     */
    @UpperCase
    @Column(name = "ex_tipi", length = 2)
    private String exTipi;

    /**
     * Peso líquido em kg — usado no grupo de transporte da NF-e.
     */
    @Column(name = "peso_liquido", precision = 12, scale = 4)
    private BigDecimal pesoLiquido;

    /**
     * Peso bruto em kg — usado no grupo de transporte da NF-e.
     */
    @Column(name = "peso_bruto", precision = 12, scale = 4)
    private BigDecimal pesoBruto;

    /**
     * CSOSN — Código de Situação da Operação no Simples Nacional.
     * Substitui o CST do ICMS no regime do Simples. Default {@code "102"}
     * (tributada pelo Simples sem permissão de crédito — venda a consumidor final).
     */
    @UpperCase
    @Column(name = "csosn", length = 3)
    private String csosn;

    /**
     * Alíquota do ICMS-ST — apenas quando o produto é sujeito à substituição
     * tributária. Opcional.
     */
    @Column(name = "aliquota_icms_st", precision = 5, scale = 2)
    private BigDecimal aliquotaIcmsSt;

    /**
     * MVA (Margem de Valor Adicionado) para cálculo da ST. Opcional.
     */
    @Column(name = "mva_st", precision = 5, scale = 2)
    private BigDecimal mvaSt;

    /**
     * CST do IPI — no Simples Nacional usa-se {@code "99"} (tributação pelo
     * regime único, sem destaque). Default {@code "99"}.
     */
    @UpperCase
    @Column(name = "cst_ipi", length = 2)
    private String cstIpi;

    /**
     * Classe de enquadramento do IPI (5 dígitos). Opcional — só se IPI relevante.
     */
    @UpperCase
    @Column(name = "classe_enq_ipi", length = 5)
    private String classeEnqIpi;

    /**
     * CST do PIS — no Simples Nacional usa-se {@code "49"} (Outras operações
     * de saída, sem destaque, pois o tributo está embutido no DAS).
     * Default {@code "49"}.
     */
    @UpperCase
    @Column(name = "cst_pis", length = 2)
    private String cstPis;

    /**
     * CST do COFINS — no Simples Nacional usa-se {@code "49"} (mesma lógica do PIS).
     * Default {@code "49"}.
     */
    @UpperCase
    @Column(name = "cst_cofins", length = 2)
    private String cstCofins;

    /**
     * Inicialização do produto antes de persistir. Garante defaults de status
     * (ATIVO) e dos campos fiscais do Simples Nacional quando não informados.
     * Não sobrescreve valores já definidos pelo chamador.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = ProductStatus.ATIVO;
        }
        if (origem == null) {
            origem = OrigemProduto.NACIONAL;
        }
        if (csosn == null) {
            csosn = "102";
        }
        if (cstIpi == null) {
            cstIpi = "99";
        }
        if (cstPis == null) {
            cstPis = "49";
        }
        if (cstCofins == null) {
            cstCofins = "49";
        }
    }
}