package br.com.toppower.erp_toppower.contract.repository;

import br.com.toppower.erp_toppower.contract.entity.ContractServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractServiceItemRepository
        extends JpaRepository<ContractServiceItem, Long> {

    /**
     * Retorna todos os itens de serviço de um contrato, ordenados pela
     * data de criação (primeiro inserido primeiro).
     */
    List<ContractServiceItem> findByContractIdOrderByCreatedAtAsc(Long contractId);

    /**
     * Remove todos os itens de serviço de um contrato. Usado em
     * substituição completa da lista (update).
     */
    void deleteByContractId(Long contractId);
}
