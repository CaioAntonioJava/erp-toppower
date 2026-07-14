package br.com.toppower.erp_toppower.sales.technicalproposal.repository;

import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicalProposalObjectiveRepository
        extends JpaRepository<TechnicalProposalObjective, Long> {

    /**
     * Retorna todos os objetivos de uma proposta, ordenados pela data de
     * criação (primeiro inserido primeiro).
     */
    List<TechnicalProposalObjective> findByTechnicalProposalIdOrderByCreatedAtAsc(Long technicalProposalId);

    /**
     * Remove todos os objetivos de uma proposta. Usado em substituição
     * completa da lista de objetivos (update).
     */
    void deleteByTechnicalProposalId(Long technicalProposalId);
}