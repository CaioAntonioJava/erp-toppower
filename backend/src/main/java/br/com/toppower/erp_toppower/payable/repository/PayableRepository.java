package br.com.toppower.erp_toppower.payable.repository;

import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayableRepository extends JpaRepository<Payable, Long>,
        JpaSpecificationExecutor<Payable> {

    /**
     * Busca a conta a pagar vinculada a um boleto (a primeira não
     * cancelada, se houver). Usado para idempotência da geração
     * automática a partir de boletos.
     */
    @Query("""
            SELECT p FROM Payable p
            WHERE p.boletoId = :boletoId
              AND p.status <> br.com.toppower.erp_toppower.payable.enums.PayableStatus.CANCELADO
            """)
    Optional<Payable> findActiveByBoletoId(@Param("boletoId") Long boletoId);
}