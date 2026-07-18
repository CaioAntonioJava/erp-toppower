package br.com.toppower.erp_toppower.receivable.repository;

import br.com.toppower.erp_toppower.receivable.entity.Receivable;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, Long>,
        JpaSpecificationExecutor<Receivable> {

    Page<Receivable> findByStatus(ReceivableStatus status, Pageable pageable);

    /**
     * Busca a conta a receber vinculada a um pedido de venda (uma por
     * pedido — a primeira não cancelada, se houver mais de uma por
     * algum motivo histórico).
     */
    @Query("""
            SELECT r FROM Receivable r
            WHERE r.salesOrderId = :salesOrderId
              AND r.status <> br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus.CANCELADO
            """)
    Optional<Receivable> findActiveBySalesOrderId(@Param("salesOrderId") Long salesOrderId);

    /**
     * Busca a conta a receber vinculada a uma proposta técnica (a
     * primeira não cancelada).
     */
    @Query("""
            SELECT r FROM Receivable r
            WHERE r.technicalProposalId = :technicalProposalId
              AND r.status <> br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus.CANCELADO
            """)
    Optional<Receivable> findActiveByTechnicalProposalId(@Param("technicalProposalId") Long technicalProposalId);

    /**
     * Busca a conta a receber vinculada a um contrato (a primeira não
     * cancelada).
     */
    @Query("""
            SELECT r FROM Receivable r
            WHERE r.contractId = :contractId
              AND r.status <> br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus.CANCELADO
            """)
    Optional<Receivable> findActiveByContractId(@Param("contractId") Long contractId);
}