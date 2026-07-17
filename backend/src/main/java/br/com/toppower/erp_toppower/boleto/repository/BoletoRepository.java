package br.com.toppower.erp_toppower.boleto.repository;

import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoletoRepository extends JpaRepository<Boleto, Long>,
        JpaSpecificationExecutor<Boleto> {

    boolean existsByDocumentNumber(String documentNumber);

    Page<Boleto> findByStatus(RegistrationStatus status, Pageable pageable);

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>{@code query} nulo/blank → ignora o filtro de texto</li>
     *   <li>{@code status} nulo → ignora o filtro de status</li>
     *   <li>Ambos nulos → retorna todos os boletos (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, busca case-insensitive em
     * {@code documentNumber} ou {@code payee}.
     */
    @Query("""
            SELECT b FROM Boleto b
            WHERE (:status IS NULL OR b.status = :status)
              AND (:query IS NULL
                OR LOWER(b.documentNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.payee) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Boleto> searchByQuery(@Param("status") RegistrationStatus status,
                               @Param("query") String query,
                               Pageable pageable);
}