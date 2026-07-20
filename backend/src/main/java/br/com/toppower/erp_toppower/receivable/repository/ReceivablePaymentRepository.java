package br.com.toppower.erp_toppower.receivable.repository;

import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivablePaymentRepository extends JpaRepository<ReceivablePayment, Long> {

    /**
     * Lista os pagamentos de uma conta ordenados pela data de pagamento
     * (asc) para apresentação do histórico.
     */
    List<ReceivablePayment> findByReceivableIdOrderByPaymentDateAsc(Long receivableId);

    /**
     * Lista os pagamentos cuja data de pagamento está no intervalo
     * [from, to] (inclusive). Usado pelo relatório de fluxo de
     * recebimentos.
     */
    List<ReceivablePayment> findByPaymentDateBetween(LocalDate from, LocalDate to);

    /**
     * Soma o valor de todos os pagamentos de uma conta. Retorna zero
     * quando a conta não possui pagamentos.
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM ReceivablePayment p
            WHERE p.receivableId = :receivableId
            """)
    BigDecimal sumAmountByReceivableId(@Param("receivableId") Long receivableId);

    /**
     * Busca um pagamento específico vinculado a uma conta — usado para
     * garantir que um {@code paymentId} pertence ao {@code receivableId}
     * informado na rota.
     */
    Optional<ReceivablePayment> findByIdAndReceivableId(Long id, Long receivableId);
}