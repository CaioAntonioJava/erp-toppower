package br.com.toppower.erp_toppower.servicecategory.repository;

import br.com.toppower.erp_toppower.servicecategory.entity.ServiceCategory;
import br.com.toppower.erp_toppower.servicecategory.enums.ServiceCategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    Page<ServiceCategory> findByStatus(ServiceCategoryStatus status, Pageable pageable);

    List<ServiceCategory> findAllByStatus(ServiceCategoryStatus status);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("""
            SELECT c FROM ServiceCategory c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:query IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<ServiceCategory> searchByQuery(@Param("status") ServiceCategoryStatus status,
                                        @Param("query") String query,
                                        Pageable pageable);
}