package br.com.toppower.erp_toppower.company.repository;

import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.company.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByCode(String code);

    boolean existsByCnpj(String cnpj);

    Optional<Company> findByCode(String code);

    Optional<Company> findByCnpj(String cnpj);

    Page<Company> findByStatus(RegistrationStatus status, Pageable pageable);

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>{@code query} nulo/blank → ignora o filtro de texto</li>
     *   <li>{@code status} nulo → ignora o filtro de status</li>
     *   <li>Ambos nulos → retorna todas as empresas (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, busca case-insensitive em
     * {@code code}, {@code legalName}, {@code tradeName} ou {@code cnpj}.
     */
    @Query("""
            SELECT c FROM Company c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:query IS NULL
                OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.legalName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.tradeName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.cnpj) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Company> searchByQuery(@Param("status") RegistrationStatus status,
                                @Param("query") String query,
                                Pageable pageable);
}
