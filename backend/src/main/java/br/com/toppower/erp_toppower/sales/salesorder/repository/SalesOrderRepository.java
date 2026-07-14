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

    boolean existsByNumber(Long number);

    Optional<SalesOrder> findByNumber(Long number);

    /**
     * Busca um pedido pelo número dentro de uma Organization específica.
     * Necessário porque, com a numeração por empresa, o mesmo número pode
     * existir em Organizations diferentes — a busca global por número não
     * distingue a empresa emissora.
     */
    Optional<SalesOrder> findByNumberAndOrganizationId(Long number, Long organizationId);

    /**
     * Retorna o maior número de pedido já emitido para a Organization
     * informada. Usado para gerar o próximo número sequencial (a partir
     * de {@code 1000} no primeiro pedido da empresa), de forma
     * independente por Organization (multi-empresa).
     *
     * <p>Retorna {@code null} quando ainda não houver nenhum pedido
     * para a Organization informada. Nesse caso, o serviço usa
     * {@code 1000} como ponto de partida.</p>
     *
     * <p>O filtro é explícito no WHERE (em vez de depender do
     * {@code organizationFilter} do Hibernate) porque o Hibernate não
     * aplica {@code @Filter} de forma confiável em queries agregadas
     * (MAX). Segue o mesmo padrão de
     * {@code TechnicalProposalRepository.findMaxSequenceByYearAndOrganizationUuid}.</p>
     */
    @Query("SELECT MAX(o.number) FROM SalesOrder o WHERE o.organizationId = :organizationId")
    Long findMaxNumberByOrganizationId(@Param("organizationId") Long organizationId);
}