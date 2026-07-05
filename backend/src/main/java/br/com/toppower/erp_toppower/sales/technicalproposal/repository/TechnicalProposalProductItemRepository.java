package br.com.toppower.erp_toppower.sales.technicalproposal.repository;

import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalProductItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TechnicalProposalProductItemRepository
        extends JpaRepository<TechnicalProposalProductItem, UUID> {

    /**
     * Retorna todos os itens de produto de uma proposta, ordenados pela
     * data de criação (primeiro item inserido primeiro).
     */
    List<TechnicalProposalProductItem> findByTechnicalProposalUuidOrderByCreatedAtAsc(UUID technicalProposalUuid);

    /**
     * Remove todos os itens de produto de uma proposta. Usado em
     * substituição completa da lista de produtos (update).
     */
    void deleteByTechnicalProposalUuid(UUID technicalProposalUuid);
}