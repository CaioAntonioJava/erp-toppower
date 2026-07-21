package br.com.toppower.erp_toppower.payable.repository;

import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayablePaymentRepository extends JpaRepository<PayablePayment, Long> {

    /**
     * Lista os pagamentos de uma conta a pagar ordenados pela data de
     * pagamento (asc) para apresentação do histórico.
     */
    List<PayablePayment> findByPayableIdOrderByPaymentDateAsc(Long payableId);

    /**
     * Lista os pagamentos cuja data de pagamento está no intervalo
     * [from, to] (inclusive). Usado pelo relatório de fluxo de
     * pagamentos.
     */
    List<PayablePayment> findByPaymentDateBetween(LocalDate from, LocalDate to);

    /**
     * Soma o valor de todos os pagamentos de uma conta a pagar.
     * Retorna zero quando a conta não possui pagamentos.
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM PayablePayment p
            WHERE p.payableId = :payableId
            """)
    BigDecimal sumAmountByPayableId(@Param("payableId") Long payableId);

    /**
     * Soma o valor de todos os pagamentos vinculados a uma parcela.
     * Retorna zero quando a parcela não possui pagamentos.
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM PayablePayment p
            WHERE p.installmentId = :installmentId
            """)
    BigDecimal sumAmountByInstallmentId(@Param("installmentId") Long installmentId);

    /**
     * Busca um pagamento específico vinculado a uma conta — usado para
     * garantir que um {@code paymentId} pertence ao {@code payableId}
     * informado na rota.
     */
    Optional<PayablePayment> findByIdAndPayableId(Long id, Long payableId);

    /**
     * Lista os pagamentos de uma parcela ordenados pela data de
     * pagamento (asc).
     */
    List<PayablePayment> findByInstallmentIdOrderByPaymentDateAsc(Long installmentId);
}