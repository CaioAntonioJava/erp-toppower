package br.com.toppower.erp_toppower.contract.repository;

import br.com.toppower.erp_toppower.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link Contract}.
 *
 * <p>Herdando {@link JpaSpecificationExecutor}, o service pode compor
 * filtros dinâmicos (status, intervalo de datas, cliente) através de
 * {@link org.springframework.data.jpa.domain.Specification}.</p>
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID>,
        JpaSpecificationExecutor<Contract> {

    /**
     * Busca um contrato pelo código formatado decomposto nos campos
     * persistidos ({@code prefix}, {@code sequence}, {@code year}).
     */
    Optional<Contract> findByPrefixAndSequenceAndYear(String prefix, Long sequence, Integer year);

    /**
     * Retorna o maior número de sequência já emitido para o ano e
     * Organization informados. Usado para gerar o próximo código
     * sequencial (reseta a {@code 1} quando o ano muda; cada Organization
     * tem sua própria sequência).
     *
     * <p>Retorna {@code null} quando ainda não houver nenhum contrato
     * para a combinação (year, organization_uuid) informada. Nesse caso,
     * o serviço usa {@code 1} como ponto de partida.</p>
     */
    @Query("""
            SELECT MAX(c.sequence) FROM Contract c
            WHERE c.year = :year AND c.organizationUuid = :organizationUuid
            """)
    Long findMaxSequenceByYearAndOrganizationUuid(Integer year, UUID organizationUuid);
}