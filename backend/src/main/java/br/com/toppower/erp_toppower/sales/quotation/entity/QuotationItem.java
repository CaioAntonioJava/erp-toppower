package br.com.toppower.erp_toppower.sales.quotation.entity;

import br.com.toppower.erp_toppower.common.entity.TenantScopedEntity;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linha de uma proposta comercial: um produto (referenciado por UUID)
 * com sua quantidade, preço unitário e desconto próprios.
 *
 * <p>Cada item pertence a uma única {@link Quotation}, identificada por
 * {@link #quotationUuid}. A relação não é mapeada via JPA (o projeto
 * não utiliza relacionamentos JPA para coleções); o serviço carrega
 * os itens com um {@code findByQuotationUuid} no repositório de itens.</p>
 *
 * <p>O campo {@link #totalPrice} armazena o <b>total líquido</b> da
 * linha, resultado de {@code (unitPrice * quantity) - discount}, onde
 * {@code discount} é interpretado conforme {@link #discountType}
 * (valor fixo em R$ ou percentual). Esse é o valor que entra no
 * somatório do {@code subtotal} da {@code Quotation}, de modo que
 * o desconto por item afeta diretamente o total final.</p>
 */
@Entity
@Table(
        name = "quotation_items",
        indexes = {
                @Index(name = "idx_quotation_item_quotation", columnList = "quotation_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class QuotationItem extends TenantScopedEntity {

    /**
     * UUID da {@link Quotation} à qual este item pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "quotation_uuid", nullable = false, updatable = false)
    private UUID quotationUuid;

    /**
     * UUID do {@code Product} referenciado pela linha.
     * Obrigatório.
     */
    @Column(name = "product_uuid", nullable = false)
    private UUID productUuid;

    /**
     * Quantidade do produto nesta linha. Suporta até 4 casas decimais
     * para produtos vendidos em unidades fracionárias (ex.: metros).
     */
    @Column(name = "quantity", nullable = false, precision = 10, scale = 4)
    private BigDecimal quantity;

    /**
     * Preço unitário do produto <b>no momento da emissão</b> da proposta
     * (snapshot). Não é atualizado quando o preço do {@code Product}
     * muda depois.
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Tipo de aplicação do desconto desta linha ({@link #discount}).
     * Nulo quando não há desconto.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    /**
     * Valor do desconto aplicado a esta linha.
     *
     * <p>Interpretado de acordo com {@link #discountType}: valor fixo
     * (R$) ou percentual (%). Nulo quando não há desconto.</p>
     */
    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount;

    /**
     * Total <b>líquido</b> da linha: {@code unitPrice * quantity} menos
     * o desconto desta linha (quando houver).
     *
     * <p>Calculado e persistido pelo serviço no momento de criar ou
     * atualizar o item. É este o valor que entra no somatório do
     * {@code subtotal} da proposta, garantindo que o desconto por
     * item afete o total final.</p>
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}
