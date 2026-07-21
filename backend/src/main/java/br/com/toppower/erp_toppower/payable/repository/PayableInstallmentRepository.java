package br.com.toppower.erp_toppower.payable.repository;

import br.com.toppower.erp_toppower.payable.entity.PayableInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayableInstallmentRepository extends JpaRepository<PayableInstallment, Long> {

    /**
     * Lista as parcelas de uma conta a pagar ordenadas pelo número da
     * parcela (asc) para apresentação consistente.
     */
    List<PayableInstallment> findByPayableIdOrderByInstallmentNumberAsc(Long payableId);

    /**
     * Busca uma parcela específica vinculada a uma conta — usado para
     * garantir que um {@code installmentId} pertence ao
     * {@code payableId} informado na rota.
     */
    Optional<PayableInstallment> findByIdAndPayableId(Long id, Long payableId);

    /**
     * Soma o valor de todas as parcelas de uma conta a pagar. Retorna
     * null quando a conta não possui parcelas.
     */
    @Query("""
            SELECT COALESCE(SUM(i.amount), 0)
            FROM PayableInstallment i
            WHERE i.payableId = :payableId
            """)
    BigDecimal sumAmountByPayableId(@Param("payableId") Long payableId);
}