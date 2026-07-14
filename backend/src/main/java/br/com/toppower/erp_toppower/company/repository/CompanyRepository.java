package br.com.toppower.erp_toppower.company.repository;

import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.company.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>,
        JpaSpecificationExecutor<Company> {

    boolean existsByCode(String code);

    boolean existsByCnpj(String cnpj);

    Optional<Company> findByCode(String code);

    Optional<Company> findByCnpj(String cnpj);

    Page<Company> findByStatus(RegistrationStatus status, Pageable pageable);

    /**
     * Retorna o maior código existente cuja string começa com o prefixo
     * informado (ex.: {@code "EMP"}). Usado para gerar o próximo código
     * sequencial (ex.: {@code EMP000001}) a partir do maior já cadastrado.
     *
     * <p>Retorna {@code null} quando ainda não houver registros com o prefixo.</p>
     *
     * <p><b>Consulta nativa</b> (não-JPQL): o {@code OrganizationFilterAspect}
     * habilita o filtro Hibernate {@code organizationFilter} em toda chamada a
     * repositório Spring Data, escopando queries JPQL/Criteria por
     * {@code organization_uuid}. Aqui isso seria indesejado: a coluna
     * {@code code} tem constraint UNIQUE <b>global</b>, então a sequência deve
     * ser contínua entre todas as Organizations — caso contrário, a segunda
     * empresa reiniciaria em {@code EMP000001} e colidiria com a primeira.
     * Consultas nativas não recebem o filtro Hibernate, garantindo o MAX
     * global.</p>
     */
    @Query(value = "SELECT MAX(c.code) FROM companies c WHERE c.code LIKE CONCAT(:prefix, '%')",
            nativeQuery = true)
    String findMaxCodeByPrefix(@Param("prefix") String prefix);

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
