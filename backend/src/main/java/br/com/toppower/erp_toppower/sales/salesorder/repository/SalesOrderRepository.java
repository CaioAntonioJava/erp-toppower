package br.com.toppower.erp_toppower.sales.salesorder.repository;

import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>,
        JpaSpecificationExecutor<SalesOrder> {

    /**
     * Busca um pedido pelo código formatado decomposto nos campos
     * persistidos ({@code prefix}, {@code sequence}, {@code year}).
     */
    Optional<SalesOrder> findByPrefixAndSequenceAndYear(String prefix, Long sequence, Integer year);

    /**
     * Retorna o maior número de sequência já emitido para o ano e
     * Organization informados. Usado para gerar o próximo código
     * sequencial (reseta a {@code 1} quando o ano muda — ou a
     * {@code INITIAL_SEQUENCE} no primeiro ano de operação; cada
     * Organization tem sua própria sequência).
     *
     * <p>Retorna {@code null} quando ainda não houver nenhum pedido
     * para a combinação (year, organization_id) informada.</p>
     *
     * <p>O filtro é explícito no WHERE (em vez de depender do
     * {@code organizationFilter} do Hibernate) porque o Hibernate não
     * aplica {@code @Filter} de forma confiável em queries agregadas
     * (MAX). Segue o mesmo padrão de
     * {@code TechnicalProposalRepository.findMaxSequenceByYearAndOrganizationId}.</p>
     */
    @Query("""
            SELECT MAX(o.sequence) FROM SalesOrder o
            WHERE o.year = :year AND o.organizationId = :organizationId
            """)
    Long findMaxSequenceByYearAndOrganizationId(@Param("year") Integer year,
                                                @Param("organizationId") Long organizationId);
}