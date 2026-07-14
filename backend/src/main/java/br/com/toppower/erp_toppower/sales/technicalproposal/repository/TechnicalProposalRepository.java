package br.com.toppower.erp_toppower.sales.technicalproposal.repository;

import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TechnicalProposalRepository extends JpaRepository<TechnicalProposal, Long>,
        JpaSpecificationExecutor<TechnicalProposal> {

    /**
     * Busca uma proposta pelo código formatado decomposto nos campos
     * persistidos ({@code prefix}, {@code sequence}, {@code year}).
     */
    Optional<TechnicalProposal> findByPrefixAndSequenceAndYear(String prefix, Long sequence, Integer year);

    /**
     * Retorna o maior número de sequência já emitido para o ano e
     * Organization informados. Usado para gerar o próximo código
     * sequencial (reseta a {@code 1} quando o ano muda; cada
     * Organization tem sua própria sequência).
     *
     * <p>Retorna {@code null} quando ainda não houver nenhuma proposta
     * para a combinação (year, organization_uuid) informada. Nesse
     * caso, o serviço usa {@code 1} como ponto de partida.</p>
     */
    @Query("""
            SELECT MAX(t.sequence) FROM TechnicalProposal t
            WHERE t.year = :year AND t.organizationId = :organizationId
            """)
    Long findMaxSequenceByYearAndOrganizationId(Integer year, Long organizationId);
}