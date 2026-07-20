package br.com.toppower.erp_toppower.sales.technicalproposal.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Linha de produto de uma proposta técnica: um produto (referenciado por
 * ID) com sua quantidade e preço unitário próprios.
 *
 * <p>Análogo ao {@code QuotationItem} do módulo de propostas comerciais,
 * mas sem margem de lucro por item (a proposta técnica não carrega
 * conceito de margem). O campo {@link #totalPrice} armazena o total da
 * linha, resultado de {@code unitPrice * quantity}. Esse é o valor que
 * entra no somatório do subtotal da {@link TechnicalProposal}.</p>
 */
@Entity
@Table(
        name = "technical_proposal_product_items",
        indexes = {
                @Index(name = "idx_tp_product_item_proposal", columnList = "technical_proposal_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TechnicalProposalProductItem extends OrganizationScopedEntity {

    /**
     * ID da {@link TechnicalProposal} à qual este item pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "technical_proposal_id", nullable = false, updatable = false)
    private Long technicalProposalId;

    /**
     * ID do {@code Product} referenciado pela linha. Obrigatório.
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

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
     * Total da linha: {@code unitPrice * quantity}. Calculado e
     * persistido pelo serviço no momento de criar ou atualizar o item.
     * É este o valor que entra no somatório do subtotal da proposta.
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}