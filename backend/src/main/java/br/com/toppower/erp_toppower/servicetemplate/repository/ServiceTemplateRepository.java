package br.com.toppower.erp_toppower.servicetemplate.repository;

import br.com.toppower.erp_toppower.servicetemplate.entity.ServiceTemplate;
import br.com.toppower.erp_toppower.servicetemplate.enums.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceTemplateRepository extends JpaRepository<ServiceTemplate, Long> {

    Page<ServiceTemplate> findByCategory(ServiceCategory category, Pageable pageable);
}
