package br.com.toppower.erp_toppower.auth.bootstrap;

import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.entity.UserTenant;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import br.com.toppower.erp_toppower.user.repository.UserTenantRepository;
import br.com.toppower.erp_toppower.tenant.entity.Tenant;
import br.com.toppower.erp_toppower.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Bootstrap inicial: garante que exista ao menos um tenant (empresa operadora)
 * default e um usuário ADMIN vinculado a ele, para o primeiro login.
 *
 * <p>Idempotente: só cria quando as tabelas estão vazias. Em produção, defina
 * as variáveis de ambiente {@code APP_BOOTSTRAP_*} antes de subir.</p>
 */
@Component
@Order(0)
public class BootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    /** UUID fixo do tenant default criado pelo bootstrap (determinístico para a migration V11 referenciar). */
    static final UUID DEFAULT_TENANT_UUID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");

    private final UserRepository userRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminEmail;
    private final String adminPassword;
    private final boolean bootstrapEnabled;
    private final String tenantLegalName;
    private final String tenantTradeName;
    private final String tenantCnpj;

    public BootstrapRunner(UserRepository userRepository,
                           UserTenantRepository userTenantRepository,
                           TenantRepository tenantRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap.admin.email:admin@toppower.com.br}") String adminEmail,
                           @Value("${app.bootstrap.admin.password:Admin@123}") String adminPassword,
                           @Value("${app.bootstrap.enabled:true}") boolean bootstrapEnabled,
                           @Value("${app.bootstrap.tenant.legal-name:TOPPOWER ENGENHARIA LTDA}") String tenantLegalName,
                           @Value("${app.bootstrap.tenant.trade-name:TOPPOWER}") String tenantTradeName,
                           @Value("${app.bootstrap.tenant.cnpj:00.000.000/0001-00}") String tenantCnpj) {
        this.userRepository = userRepository;
        this.userTenantRepository = userTenantRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.bootstrapEnabled = bootstrapEnabled;
        this.tenantLegalName = tenantLegalName;
        this.tenantTradeName = tenantTradeName;
        this.tenantCnpj = tenantCnpj;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!bootstrapEnabled) {
            log.info("Bootstrap desabilitado (app.bootstrap.enabled=false). Nada a fazer.");
            return;
        }

        // 1) Garante o tenant default.
        Tenant defaultTenant = tenantRepository.findById(DEFAULT_TENANT_UUID).orElseGet(() -> {
            log.warn("=================================================================");
            log.warn(" TENANT DEFAULT AUSENTE: criando tenant padrão.");
            log.warn("   Razão social: {}", tenantLegalName);
            log.warn("   CNPJ:         {}", tenantCnpj);
            log.warn("   -> Sobrescreva via APP_BOOTSTRAP_TENANT_* no .env em produção.");
            log.warn("=================================================================");
            Tenant t = new Tenant();
            t.setUuid(DEFAULT_TENANT_UUID);
            t.setLegalName(tenantLegalName);
            t.setTradeName(tenantTradeName);
            t.setCnpj(tenantCnpj);
            t.setCode("TEN000001");
            // Endereço placeholder (obrigatório na entidade). O tenant default
            // é um placeholder para o primeiro login; será substituído/inativado
            // pelos tenants reais cadastrados via API.
            br.com.toppower.erp_toppower.common.embeddable.Address addr =
                    new br.com.toppower.erp_toppower.common.embeddable.Address();
            addr.setStreet("ENDEREÇO PLACEHOLDER");
            addr.setNumber("S/N");
            addr.setCity("São Paulo");
            addr.setState("SP");
            addr.setZipCode("00000-000");
            t.setAddress(addr);
            return tenantRepository.save(t);
        });

        // 2) Garante o ADMIN default (se ainda não houver usuários).
        if (userRepository.count() > 0) {
            log.info("Tabela 'users' já possui registros. Bootstrap de admin não necessário.");
            ensureAdminTenantLink(defaultTenant);
            return;
        }

        log.warn("=================================================================");
        log.warn(" TABELA DE USUÁRIOS VAZIA: criando administrador padrão.");
        log.warn("   E-mail:  {}", adminEmail);
        log.warn("   Senha:   {}", adminPassword);
        log.warn("   Tenant:  {} ({})", defaultTenant.getCode(), defaultTenant.getLegalName());
        log.warn("   -> Use esta credencial + o tenant acima para o primeiro login.");
        log.warn("   -> Defina APP_BOOTSTRAP_ADMIN_PASSWORD=<outra-senha> no .env antes de subir para produção.");
        log.warn("=================================================================");

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);

        UserTenant link = new UserTenant(admin.getUuid(), defaultTenant.getUuid());
        userTenantRepository.save(link);

        log.info("Administrador padrão criado e vinculado ao tenant default: {}", adminEmail);
    }

    /**
     * Cenário: usuários já existem, mas o admin default pode não estar vinculado
     * ao tenant default (ex: banco legado pré-multi-tenancy). Garante o vínculo
     * se faltar, para o admin não ficar sem acesso a nenhum tenant.
     */
    private void ensureAdminTenantLink(Tenant defaultTenant) {
        userRepository.findByEmail(adminEmail).ifPresent(admin -> {
            boolean alreadyLinked = userTenantRepository
                    .existsByUserUuidAndTenantUuid(admin.getUuid(), defaultTenant.getUuid());
            if (!alreadyLinked) {
                userTenantRepository.save(new UserTenant(admin.getUuid(), defaultTenant.getUuid()));
                log.info("Vínculo do admin '{}' com o tenant default criado (ausência de vínculo detectada).",
                        adminEmail);
            }
        });
    }
}