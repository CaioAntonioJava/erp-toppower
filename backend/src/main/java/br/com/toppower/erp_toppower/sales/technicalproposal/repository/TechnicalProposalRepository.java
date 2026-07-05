package br.com.toppower.erp_toppower.sales.technicalproposal.repository;

import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TechnicalProposalRepository extends JpaRepository<TechnicalProposal, UUID>,
        JpaSpecificationExecutor<TechnicalProposal> {

    /**
     * Busca uma proposta pelo código formatado decomposto nos campos
     * persistidos ({@code prefix}, {@code sequence}, {@code year}).
     */
    Optional<TechnicalProposal> findByPrefixAndSequenceAndYear(String prefix, Long sequence, Integer year);

    /**
     * Retorna o maior número de sequência já emitido para o ano informado.
     * Usado para gerar o próximo código sequencial (reseta a {@code 1}
     * quando o ano muda).
     *
     * <p>Retorna {@code null} quando ainda não houver nenhuma proposta
     * para o ano informado. Nesse caso, o serviço usa {@code 1} como
     * ponto de partida.</p>
     */
    @Query("SELECT MAX(t.sequence) FROM TechnicalProposal t WHERE t.year = :year")
    Long findMaxSequenceByYear(Integer year);
}