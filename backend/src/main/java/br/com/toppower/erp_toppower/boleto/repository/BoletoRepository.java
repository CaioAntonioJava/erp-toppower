package br.com.toppower.erp_toppower.boleto.repository;

import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface BoletoRepository extends JpaRepository<Boleto, Long>,
        JpaSpecificationExecutor<Boleto> {

    Page<Boleto> findByStatus(RegistrationStatus status, Pageable pageable);

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>{@code query} nulo/blank → ignora o filtro de texto</li>
     *   <li>{@code status} nulo → ignora o filtro de status</li>
     *   <li>Ambos nulos → retorna todos os boletos (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, busca case-insensitive em
     * {@code description} ou {@code payee}.
     */
    @Query("""
            SELECT b FROM Boleto b
            WHERE (:status IS NULL OR b.status = :status)
              AND (:query IS NULL
                OR LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.payee) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Boleto> searchByQuery(@Param("status") RegistrationStatus status,
                               @Param("query") String query,
                               Pageable pageable);

    /**
     * Specification combinável para o relatório de boletos: filtra por
     * status de registro e status de pagamento (paid). Todos os parâmetros
     * são opcionais (null = ignorar o filtro). O escopo de organização é
     * aplicado automaticamente pelo OrganizationFilter em entidades
     * escopadas.
     *
     * <p><b>Intervalo de datas (dueFrom/dueTo):</b> aplicado
     * <b>exclusivamente</b> sobre a <b>data de pagamento</b>
     * ({@code paymentDate}) e somente quando o relatório é de boletos
     * <i>pagos</i> ({@code paid=true}). Para "em aberto" ou "todos" o
     * intervalo é ignorado — boletos em aberto costumam ter vencimento no
     * futuro, então filtrar "em aberto + hoje/últimos 7 dias" retornaria
     * sempre vazio de forma confusa. Quem quer ver boletos pagos em um
     * período (Hoje, Últimos 7 dias...) usa o filtro "Pagos" + o período.</p>
     */
    static org.springframework.data.jpa.domain.Specification<Boleto> byFilters(
            RegistrationStatus status,
            Boolean paid,
            LocalDate dueFrom,
            LocalDate dueTo) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (paid != null) {
                predicates.add(cb.equal(root.get("paid"), paid));
            }
            // O intervalo de datas só faz sentido para boletos pagos
            // (filtra pela data de pagamento). Nos demais casos é ignorado.
            if (Boolean.TRUE.equals(paid)) {
                if (dueFrom != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), dueFrom));
                }
                if (dueTo != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), dueTo));
                }
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}