package br.com.toppower.erp_toppower.contract.repository;

import br.com.toppower.erp_toppower.contract.entity.ContractClause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractClauseRepository
        extends JpaRepository<ContractClause, Long> {

    /**
     * Retorna todas as cláusulas de um contrato, ordenadas pela data de
     * criação (primeiro inserido primeiro).
     */
    List<ContractClause> findByContractIdOrderByCreatedAtAsc(Long contractId);

    /**
     * Remove todas as cláusulas de um contrato. Usado em substituição
     * completa da lista de cláusulas (update).
     */
    void deleteByContractId(Long contractId);
}
