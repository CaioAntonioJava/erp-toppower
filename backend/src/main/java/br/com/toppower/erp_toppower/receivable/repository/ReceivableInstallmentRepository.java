package br.com.toppower.erp_toppower.receivable.repository;

import br.com.toppower.erp_toppower.receivable.entity.ReceivableInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivableInstallmentRepository extends JpaRepository<ReceivableInstallment, Long> {

    /**
     * Lista as parcelas de uma conta a receber ordenadas pelo número da
     * parcela (asc) para apresentação consistente.
     */
    List<ReceivableInstallment> findByReceivableIdOrderByInstallmentNumberAsc(Long receivableId);

    /**
     * Busca uma parcela específica vinculada a uma conta — usado para
     * garantir que um {@code installmentId} pertence ao
     * {@code receivableId} informado na rota.
     */
    Optional<ReceivableInstallment> findByIdAndReceivableId(Long id, Long receivableId);

    /**
     * Soma o valor de todas as parcelas de uma conta a receber. Retorna
     * zero quando a conta não possui parcelas.
     */
    @Query("""
            SELECT COALESCE(SUM(i.amount), 0)
            FROM ReceivableInstallment i
            WHERE i.receivableId = :receivableId
            """)
    BigDecimal sumAmountByReceivableId(@Param("receivableId") Long receivableId);
}