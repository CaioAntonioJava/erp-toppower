package br.com.toppower.erp_toppower.sales.technicalproposal.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
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
 * Linha de produto de uma proposta técnica: um produto (referenciado por
 * UUID) com sua quantidade, preço unitário e desconto próprios.
 *
 * <p>Análogo ao {@code QuotationItem} do módulo de propostas comerciais.
 * O campo {@link #totalPrice} armazena o <b>total líquido</b> da linha,
 * resultado de {@code (unitPrice * quantity) - discount}, onde
 * {@code discount} é interpretado conforme {@link #discountType}. Esse é
 * o valor que entra no somatório do subtotal da {@link TechnicalProposal}.</p>
 */
@Entity
@Table(
        name = "technical_proposal_product_items",
        indexes = {
                @Index(name = "idx_tp_product_item_proposal", columnList = "technical_proposal_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TechnicalProposalProductItem extends OrganizationScopedEntity {

    /**
     * UUID da {@link TechnicalProposal} à qual este item pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "technical_proposal_uuid", nullable = false, updatable = false)
    private UUID technicalProposalUuid;

    /**
     * UUID do {@code Product} referenciado pela linha. Obrigatório.
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
     * (snapshot). Já reflete a margem de lucro da proposta embutida —
     * o valor final exibido no PDF e na listagem. Não é atualizado
     * quando o preço do {@code Product} muda depois.
     *
     * <p>Para edição o frontend usa {@link #baseUnitPrice} (preço
     * original enviado pelo usuário, sem margem) como ponto de partida,
     * evitando reaplicar a margem sobre o snapshot.</p>
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Preço unitário original do produto, <b>sem a margem de lucro</b>
     * aplicada. É o valor enviado pelo usuário no formulário e o ponto
     * de partida para a aplicação da margem no momento da
     * criação/atualização.
     *
     * <p>Persistido separadamente de {@link #unitPrice} para que a
     * edição da proposta não reaplique a margem sobre o snapshot já
     * majorado. O cálculo do {@link #totalPrice} continua usando
     * {@code unitPrice} (com margem).</p>
     */
    @Column(name = "base_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseUnitPrice;

    /**
     * Tipo de aplicação do desconto desta linha ({@link #discount}).
     * Nulo quando não há desconto.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    /**
     * Valor do desconto aplicado a esta linha, interpretado conforme
     * {@link #discountType} (valor fixo em R$ ou percentual). Nulo quando
     * não há desconto.
     */
    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount;

    /**
     * Total <b>líquido</b> da linha: {@code unitPrice * quantity} menos
     * o desconto desta linha (quando houver). Calculado e persistido pelo
     * serviço no momento de criar ou atualizar o item. É este o valor que
     * entra no somatório do subtotal da proposta.
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}