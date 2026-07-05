package br.com.toppower.erp_toppower.sales.technicalproposal.repository;

import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TechnicalProposalServiceItemRepository
        extends JpaRepository<TechnicalProposalServiceItem, UUID> {

    /**
     * Retorna todos os itens de serviço de uma proposta, ordenados pela
     * data de criação (primeiro item inserido primeiro).
     */
    List<TechnicalProposalServiceItem> findByTechnicalProposalUuidOrderByCreatedAtAsc(UUID technicalProposalUuid);

    /**
     * Remove todos os itens de serviço de uma proposta. Usado em
     * substituição completa da lista de serviços (update).
     */
    void deleteByTechnicalProposalUuid(UUID technicalProposalUuid);
}