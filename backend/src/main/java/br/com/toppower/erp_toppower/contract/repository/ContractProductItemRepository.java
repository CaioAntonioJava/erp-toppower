package br.com.toppower.erp_toppower.contract.repository;

import br.com.toppower.erp_toppower.contract.entity.ContractProductItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractProductItemRepository
        extends JpaRepository<ContractProductItem, UUID> {

    /**
     * Retorna todos os itens de produto de um contrato, ordenados pela
     * data de criação (primeiro inserido primeiro).
     */
    List<ContractProductItem> findByContractUuidOrderByCreatedAtAsc(UUID contractUuid);

    /**
     * Remove todos os itens de produto de um contrato. Usado em
     * substituição completa da lista (update).
     */
    void deleteByContractUuid(UUID contractUuid);
}
