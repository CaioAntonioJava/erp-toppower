package br.com.toppower.erp_toppower.organization.repository;

import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsByCnpj(String cnpj);

    /**
     * Verifica se já existe uma Organization com o prefixo de Proposta
     * Técnica informado. O prefixo é globalmente único (constraint
     * {@code uk_organizations_proposal_prefix} criada pela migration V25).
     */
    boolean existsByProposalPrefix(String proposalPrefix);

    /**
     * Verifica se já existe uma Organization com o prefixo de Contrato
     * informado. O prefixo é globalmente único (constraint
     * {@code uk_organizations_contract_prefix} criada pela migration V29).
     */
    boolean existsByContractPrefix(String contractPrefix);

    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);

    Optional<Organization> findByCnpj(String cnpj);

    /**
     * Busca Organizations pelo nome (razão social ou nome fantasia), case-insensitive.
     * Usado pelo admin na listagem/busca de empresas.
     */
    @Query("""
            SELECT o FROM Organization o
            WHERE (:status IS NULL OR o.status = :status)
              AND (:query IS NULL
                OR LOWER(o.corporateName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(o.tradeName) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Organization> search(@Param("status") OrganizationStatus status,
                              @Param("query") String query,
                              Pageable pageable);
}