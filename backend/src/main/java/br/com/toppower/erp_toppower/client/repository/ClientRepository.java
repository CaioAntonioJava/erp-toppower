package br.com.toppower.erp_toppower.client.repository;

import br.com.toppower.erp_toppower.client.entity.Client;
import br.com.toppower.erp_toppower.client.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    boolean existsByCode(String code);

    boolean existsByTaxId(String taxId);

    Optional<Client> findByCode(String code);

    Optional<Client> findByTaxId(String taxId);

    Page<Client> findByStatus(ClientStatus status, Pageable pageable);

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>{@code query} nulo/blank → ignora o filtro de texto</li>
     *   <li>{@code status} nulo → ignora o filtro de status</li>
     *   <li>Ambos nulos → retorna todos os clientes (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, busca case-insensitive em
     * {@code code}, {@code legalName}, {@code tradeName} ou {@code taxId}.
     */
    @Query("""
            SELECT c FROM Client c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:query IS NULL
                OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.legalName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.tradeName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.taxId) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Client> searchByQuery(@Param("status") ClientStatus status,
                                @Param("query") String query,
                                Pageable pageable);
}
