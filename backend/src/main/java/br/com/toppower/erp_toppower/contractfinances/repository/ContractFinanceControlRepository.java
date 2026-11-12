package br.com.toppower.erp_toppower.contractfinances.repository;

import br.com.toppower.erp_toppower.contractfinances.entity.ContractFinanceControl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository dos controles do financeiro de contratos. O isolamento por
 * organização é garantido pelo {@code organizationFilter} (entidade
 * organization-scoped).
 */
public interface ContractFinanceControlRepository
        extends JpaRepository<ContractFinanceControl, Long> {

    /** Controle vinculado a um contrato (cada contrato tem, no máximo, um). */
    Optional<ContractFinanceControl> findByContractId(Long contractId);

    /**
     * Lista paginada com busca opcional por código do contrato ou nome
     * do cliente (case-insensitive, match parcial).
     */
    @Query("""
            SELECT c FROM ContractFinanceControl c
             WHERE :query IS NULL
                OR LOWER(c.contractCode) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.clientName) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<ContractFinanceControl> searchControls(@Param("query") String query, Pageable pageable);
}
