package br.com.toppower.erp_toppower.sales.technicalproposal.entity;

import br.com.toppower.erp_toppower.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Linha de objetivo de uma proposta técnica: uma descrição livre de um
 * dos objetivos do serviço prestado.
 *
 * <p>Uma proposta técnica pode ter <b>vários</b> objetivos, cada um em
 * uma linha deste agregado. Cada item pertence a uma única
 * {@link TechnicalProposal}, identificada por
 * {@link #technicalProposalUuid}. A relação não é mapeada via JPA (o
 * projeto não utiliza relacionamentos JPA para coleções); o serviço
 * carrega os itens com um {@code findByTechnicalProposalUuid} no
 * repositório.</p>
 */
@Entity
@Table(
        name = "technical_proposal_objectives",
        indexes = {
                @Index(name = "idx_tp_objective_proposal", columnList = "technical_proposal_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TechnicalProposalObjective extends TenantScopedEntity {

    /**
     * UUID da {@link TechnicalProposal} à qual este objetivo pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "technical_proposal_uuid", nullable = false, updatable = false)
    private UUID technicalProposalUuid;

    /**
     * Descrição do objetivo do serviço prestado (texto livre).
     * Obrigatória.
     */
    @Column(name = "description", nullable = false, length = 500)
    private String description;
}