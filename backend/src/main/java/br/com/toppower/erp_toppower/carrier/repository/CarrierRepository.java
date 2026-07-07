package br.com.toppower.erp_toppower.carrier.repository;

import br.com.toppower.erp_toppower.carrier.entity.Carrier;
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

    @Query("""
            SELECT c FROM Carrier c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:query IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Carrier> searchByQuery(@Param("status") CarrierStatus status,
                                @Param("query") String query,
                                Pageable pageable);
}