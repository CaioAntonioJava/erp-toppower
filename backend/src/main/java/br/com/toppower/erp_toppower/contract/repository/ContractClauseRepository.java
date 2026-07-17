package br.com.toppower.erp_toppower.contract.repository;

import br.com.toppower.erp_toppower.contract.entity.ContractClause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository das cláusulas de um contrato.
 *
 * <p>Segue a convenção do projeto: o carregamento das cláusulas de um
 * contrato é feito explicitamente pelo serviço (sem relacionamento JPA
 * no {@code Contract}). A substituição completa (full replacement) em
 * atualizações usa {@link #deleteByContractId(Long)} + re-inserção.</p>
 */
@Repository
public interface ContractClauseRepository extends JpaRepository<ContractClause, Long> {

    /**
     * Retorna todas as cláusulas de um contrato, ordenadas pelo número
     * da cláusula em ordem ascendente (1, 2, 3, ...).
     */
    List<ContractClause> findByContractIdOrderByClauseNumberAsc(Long contractId);

    /**
     * Remove todas as cláusulas de um contrato. Usado no update
     * (full replacement) antes de re-inserir as novas cláusulas.
     */
    void deleteByContractId(Long contractId);
}