package br.com.toppower.erp_toppower.sales.technicalproposal.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Condição de uma proposta técnica: um item com título e conteúdo textual
 * que descreve uma condição específica (ex.: garantia, prazo de pagamento,
 * multa, forma de pagamento, etc.).
 *
 * <p>Cada condição pertence a uma única {@link TechnicalProposal}, identificada
 * por {@link #technicalProposalId}. A relação não é mapeada via JPA (o
 * projeto não utiliza relacionamentos JPA para coleções); o serviço carrega
 * as condições com um {@code findByTechnicalProposalIdOrderBySortOrderAsc}
 * no repositório de condições.</p>
 *
 * <p>A ordem de exibição ({@link #sortOrder}) é definida pelo usuário no
 * frontend e respeitada na renderização do PDF.</p>
 */
@Entity
@Table(
        name = "technical_proposal_conditions",
        indexes = {
                @Index(name = "idx_tp_condition_proposal", columnList = "technical_proposal_id"),
                @Index(name = "idx_tp_condition_org", columnList = "organization_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TechnicalProposalCondition extends OrganizationScopedEntity {

    /**
     * ID da {@link TechnicalProposal} à qual esta condição pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "technical_proposal_id", nullable = false, updatable = false)
    private Long technicalProposalId;

    /**
     * Título da condição (ex.: "Garantia", "Prazo de pagamento").
     * Obrigatório.
     */
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    /**
     * Conteúdo textual da condição. Opcional — pode ser nulo quando a
     * condição é apenas um título (ex.: seção de destaque).
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Ordem de exibição da condição na lista. Valores mais baixos aparecem
     * primeiro. Definida pelo usuário no frontend.
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
