package br.com.toppower.erp_toppower.contract.repository;

import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long>,
        JpaSpecificationExecutor<Contract> {

    /**
     * Busca um contrato pelo código formatado decomposto nos campos
     * persistidos ({@code prefix}, {@code sequence}, {@code year}).
     */
    Optional<Contract> findByPrefixAndSequenceAndYear(String prefix, Long sequence, Integer year);

    Page<Contract> findByStatus(ContractStatus status, Pageable pageable);

    /**
     * Retorna o maior número de sequência já emitido para o ano e
     * Organization informados. Usado para gerar o próximo código
     * sequencial (reseta a {@code 1} quando o ano muda; cada
     * Organization tem sua própria sequência).
     *
     * <p>Retorna {@code null} quando ainda não houver nenhum contrato
     * para a combinação (year, organization_uuid) informada. Nesse
     * caso, o serviço usa {@code 1} como ponto de partida.</p>
     */
    @Query("""
            SELECT MAX(c.sequence) FROM Contract c
            WHERE c.year = :year AND c.organizationId = :organizationId
            """)
    Long findMaxSequenceByYearAndOrganizationId(Integer year, Long organizationId);

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>{@code query} nulo/blank → ignora o filtro de texto</li>
     *   <li>{@code status} nulo → ignora o filtro de status</li>
     *   <li>Ambos nulos → retorna todos os contratos (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, busca case-insensitive no
     * código formatado (via prefix/sequence/year), título ou descrição.
     */
    @Query("""
            SELECT c FROM Contract c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:query IS NULL
                OR LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(CONCAT(c.prefix, '-', c.sequence, '-', c.year)) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Contract> searchByQuery(@Param("status") ContractStatus status,
                                 @Param("query") String query,
                                 Pageable pageable);
}