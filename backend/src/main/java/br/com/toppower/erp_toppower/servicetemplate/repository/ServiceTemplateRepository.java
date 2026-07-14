package br.com.toppower.erp_toppower.servicetemplate.repository;

import br.com.toppower.erp_toppower.servicetemplate.entity.ServiceTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceTemplateRepository extends JpaRepository<ServiceTemplate, Long> {

    @Query("""
            SELECT s FROM ServiceTemplate s
            WHERE :query IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<ServiceTemplate> searchByQuery(@Param("query") String query,
                                        Pageable pageable);
}
