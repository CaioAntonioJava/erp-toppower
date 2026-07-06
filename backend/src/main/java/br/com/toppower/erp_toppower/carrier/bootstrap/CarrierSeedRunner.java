package br.com.toppower.erp_toppower.carrier.bootstrap;

import br.com.toppower.erp_toppower.carrier.entity.Carrier;
import br.com.toppower.erp_toppower.carrier.enums.CarrierName;
import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import br.com.toppower.erp_toppower.carrier.repository.CarrierRepository;
import br.com.toppower.erp_toppower.common.context.TenantContext;
import br.com.toppower.erp_toppower.tenant.entity.Tenant;
import br.com.toppower.erp_toppower.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seed das transportadoras padrão do sistema.
 *
 * <p>A migration V10 purgea a tabela {@code carriers} a cada boot
 * ({@code spring.sql.init.mode=always}), o que esvaziaria o dropdown de
 * transportadoras em cotações/pedidos. Este runner recria, de forma
 * idempotente, um registro ATIVO para cada valor do enum
 * {@link CarrierName} em todos os tenants existentes.</p>
 *
 * <p>Roda após {@code BootstrapRunner} (0), {@code TenantBackfillRunner}
 * (10) e {@code CepBootstrapRunner} (20), garantindo que os tenants já
 * existam antes do seed.</p>
 */
@Component
@Order(30)
public class CarrierSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CarrierSeedRunner.class);

    private final CarrierRepository carrierRepository;
    private final TenantRepository tenantRepository;

    public CarrierSeedRunner(CarrierRepository carrierRepository,
                             TenantRepository tenantRepository) {
        this.carrierRepository = carrierRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<Tenant> tenants = tenantRepository.findAll();
        if (tenants.isEmpty()) {
            log.info("Nenhum tenant encontrado — seed de transportadoras pulado.");
            return;
        }

        int created = 0;
        for (Tenant tenant : tenants) {
            // O TenantEntityListener preenche tenant_uuid a partir do
            // TenantContext no @PrePersist. Sem setar o contexto, o
            // listener deixaria a coluna nullable e a constraint NOT NULL
            // falharia. O finally garante o limpeza do ThreadLocal.
            TenantContext.set(tenant.getUuid());
            try {
                for (CarrierName name : CarrierName.values()) {
                    if (carrierRepository.existsByCarrierNameAndTenantUuid(name, tenant.getUuid())) {
                        continue;
                    }
                    Carrier carrier = new Carrier();
                    carrier.setCarrierName(name);
                    carrier.setStatus(CarrierStatus.ATIVO);
                    carrierRepository.save(carrier);
                    created++;
                }
            } finally {
                TenantContext.clear();
            }
        }

        if (created > 0) {
            log.info("Seed de transportadoras: {} registro(s) criado(s) em {} tenant(s).",
                    created, tenants.size());
        }
    }
}