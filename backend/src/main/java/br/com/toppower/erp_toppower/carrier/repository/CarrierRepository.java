package br.com.toppower.erp_toppower.carrier.repository;

import br.com.toppower.erp_toppower.carrier.entity.Carrier;
import br.com.toppower.erp_toppower.carrier.enums.CarrierName;
import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, UUID> {

    Page<Carrier> findByStatus(CarrierStatus status, Pageable pageable);

    /**
     * Busca flexível por nome (opcional) e/ou status (opcional).
     * <ul>
     *   <li>{@code carrierName} nulo → ignora o filtro de nome</li>
     *   <li>{@code status} nulo → ignora o filtro de status</li>
     *   <li>Ambos nulos → retorna todas as transportadoras (paginado)</li>
     * </ul>
     * Como {@code carrierName} é um enum, a comparação é por igualdade
     * exata (não há sentido para match parcial de texto).
     */
    @Query("""
            SELECT c FROM Carrier c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:carrierName IS NULL OR c.carrierName = :carrierName)
            """)
    Page<Carrier> searchByQuery(@Param("status") CarrierStatus status,
                                @Param("carrierName") CarrierName carrierName,
                                Pageable pageable);
}
