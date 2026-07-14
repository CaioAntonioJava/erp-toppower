package br.com.toppower.erp_toppower.supplier.repository;

import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.enums.SupplierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByTaxId(String taxId);

    boolean existsByEmail(String email);

    Optional<Supplier> findByTaxId(String taxId);

    Page<Supplier> findByStatus(SupplierStatus status, Pageable pageable);

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * Quando ambos nulos, retorna todos (paginado).
     */
    @Query("""
            SELECT s FROM Supplier s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:query IS NULL
                OR LOWER(s.legalName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(s.tradeName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(s.taxId) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(s.contactName) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Supplier> searchByQuery(@Param("status") SupplierStatus status,
                                @Param("query") String query,
                                Pageable pageable);
}
