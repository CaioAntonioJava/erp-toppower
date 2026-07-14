package br.com.toppower.erp_toppower.contract.repository;

import br.com.toppower.erp_toppower.contract.entity.ContractProductItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractProductItemRepository
        extends JpaRepository<ContractProductItem, Long> {

    /**
     * Retorna todos os itens de produto de um contrato, ordenados pela
     * data de criação (primeiro inserido primeiro).
     */
    List<ContractProductItem> findByContractIdOrderByCreatedAtAsc(Long contractId);

    /**
     * Remove todos os itens de produto de um contrato. Usado em
     * substituição completa da lista (update).
     */
    void deleteByContractId(Long contractId);
}
