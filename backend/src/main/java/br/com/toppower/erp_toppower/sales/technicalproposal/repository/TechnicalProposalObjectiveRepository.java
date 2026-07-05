package br.com.toppower.erp_toppower.sales.technicalproposal.repository;

import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TechnicalProposalObjectiveRepository
        extends JpaRepository<TechnicalProposalObjective, UUID> {

    /**
     * Retorna todos os objetivos de uma proposta, ordenados pela data de
     * criação (primeiro inserido primeiro).
     */
    List<TechnicalProposalObjective> findByTechnicalProposalUuidOrderByCreatedAtAsc(UUID technicalProposalUuid);

    /**
     * Remove todos os objetivos de uma proposta. Usado em substituição
     * completa da lista de objetivos (update).
     */
    void deleteByTechnicalProposalUuid(UUID technicalProposalUuid);
}