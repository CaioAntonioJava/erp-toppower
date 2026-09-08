package br.com.toppower.erp_toppower.cashflow.repository;

import br.com.toppower.erp_toppower.cashflow.entity.CashFlowSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashFlowSettingsRepository extends JpaRepository<CashFlowSettings, Long> {

    /**
     * Busca a configuração de fluxo de caixa de uma organização. A query
     * JPQL derivada também é escopada pelo {@code organizationFilter}.
     */
    Optional<CashFlowSettings> findByOrganizationId(Long organizationId);
}