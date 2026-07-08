package br.com.toppower.erp_toppower.sales.quotation.repository;

import br.com.toppower.erp_toppower.sales.quotation.entity.Quotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, UUID>,
        JpaSpecificationExecutor<Quotation> {

    boolean existsByNumber(Long number);

    Optional<Quotation> findByNumber(Long number);

    /**
     * Busca uma proposta pelo número dentro de uma Organization específica.
     * Necessário porque, com a numeração por empresa, o mesmo número pode
     * existir em Organizations diferentes — a busca global por número não
     * distingue a empresa emissora.
     */
    Optional<Quotation> findByNumberAndOrganizationUuid(Long number, UUID organizationUuid);

    /**
     * Retorna o maior número de proposta já emitido para a Organization
     * informada. Usado para gerar o próximo número sequencial (a partir
     * de {@code 1500} na primeira proposta da empresa), de forma
     * independente por Organization (multi-empresa).
     *
     * <p>Retorna {@code null} quando ainda não houver nenhuma proposta
     * para a Organization informada. Nesse caso, o serviço usa
     * {@code 1500} como ponto de partida.</p>
     *
     * <p>O filtro é explícito no WHERE (em vez de depender do
     * {@code organizationFilter} do Hibernate) porque o Hibernate não
     * aplica {@code @Filter} de forma confiável em queries agregadas
     * (MAX). Segue o mesmo padrão de
     * {@code TechnicalProposalRepository.findMaxSequenceByYearAndOrganizationUuid}.</p>
     */
    @Query("SELECT MAX(q.number) FROM Quotation q WHERE q.organizationUuid = :organizationUuid")
    Long findMaxNumberByOrganizationUuid(@Param("organizationUuid") UUID organizationUuid);

    /**
     * Busca paginada por status (opcional).
     * Se {@code status} for nulo, retorna todas as propostas.
     */
    Page<Quotation> findByStatus(br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus status,
                                 Pageable pageable);
}
