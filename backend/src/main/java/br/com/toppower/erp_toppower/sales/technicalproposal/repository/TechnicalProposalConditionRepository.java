package br.com.toppower.erp_toppower.sales.technicalproposal.repository;

import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para acesso aos dados de {@link TechnicalProposalCondition}.
 */
@Repository
public interface TechnicalProposalConditionRepository
        extends JpaRepository<TechnicalProposalCondition, Long> {

    /**
     * Retorna todas as condições de uma proposta, ordenadas pelo campo
     * {@code sortOrder} (menor primeiro).
     */
    List<TechnicalProposalCondition> findByTechnicalProposalIdOrderBySortOrderAsc(Long technicalProposalId);

    /**
     * Remove todas as condições de uma proposta. Usado em substituição
     * completa da lista de condições (update).
     */
    void deleteByTechnicalProposalId(Long technicalProposalId);
}
