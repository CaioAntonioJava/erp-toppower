package br.com.toppower.erp_toppower.contractfinances.repository;

import br.com.toppower.erp_toppower.contractfinances.entity.ContractFinanceEntry;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryStatus;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository dos lançamentos do financeiro de contratos. O isolamento
 * por organização é garantido pelo {@code organizationFilter} (entidade
 * organization-scoped); as agregações por controle/tipo consideram
 * apenas lançamentos ATIVO (inativos não somam nos totais).
 */
public interface ContractFinanceEntryRepository
        extends JpaRepository<ContractFinanceEntry, Long> {

    Optional<ContractFinanceEntry> findByIdAndControlId(Long id, Long controlId);

    List<ContractFinanceEntry> findByControlIdOrderByEntryDateDesc(Long controlId);

    /**
     * Lista paginada de lançamentos com filtros combináveis (todos
     * opcionais: {@code null} = filtro inativo). Ordena por data
     * descendente, mais recente primeiro.
     */
    @Query("""
            SELECT e FROM ContractFinanceEntry e
             WHERE (:controlId IS NULL OR e.controlId = :controlId)
               AND (:type IS NULL OR e.type = :type)
               AND (:status IS NULL OR e.status = :status)
               AND (:from IS NULL OR e.entryDate >= :from)
               AND (:to IS NULL OR e.entryDate <= :to)
            """)
    Page<ContractFinanceEntry> listEntries(@Param("controlId") Long controlId,
            @Param("type") ContractFinanceEntryType type,
            @Param("status") ContractFinanceEntryStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    /**
     * Agregação por controle: [controlId, tipo, soma, quantidade] dos
     * lançamentos com o status informado (o service passa ATIVO —
     * inativos não somam nos totais) dos controles informados (para
     * compor os totais da página de controles e do detalhe).
     */
    @Query("""
            SELECT e.controlId, e.type, SUM(e.amount), COUNT(e)
              FROM ContractFinanceEntry e
             WHERE e.status = :status
               AND e.controlId IN :controlIds
             GROUP BY e.controlId, e.type
            """)
    List<Object[]> aggregateByControlIds(@Param("controlIds") Collection<Long> controlIds,
            @Param("status") ContractFinanceEntryStatus status);

    /**
     * Agregação por tipo respeitando os filtros informados (todos
     * opcionais): [tipo, soma, quantidade] dos lançamentos com o status
     * informado (o service passa ATIVO). Base do resumo
     * (recebido/gasto/resultado + por tipo).
     */
    @Query("""
            SELECT e.type, SUM(e.amount), COUNT(e)
              FROM ContractFinanceEntry e
             WHERE e.status = :status
               AND (:controlId IS NULL OR e.controlId = :controlId)
               AND (:type IS NULL OR e.type = :type)
               AND (:from IS NULL OR e.entryDate >= :from)
               AND (:to IS NULL OR e.entryDate <= :to)
             GROUP BY e.type
            """)
    List<Object[]> aggregateTotalsByType(@Param("controlId") Long controlId,
            @Param("type") ContractFinanceEntryType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") ContractFinanceEntryStatus status);
}
