package br.com.toppower.erp_toppower.auth.bootstrap;

import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import br.com.toppower.erp_toppower.userorganization.entity.UserOrganization;
import br.com.toppower.erp_toppower.userorganization.repository.UserOrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstrap inicial: garante que exista ao menos um usuário ADMIN e as
 * Organizations padrão para o primeiro login.
 *
 * <p>Idempotente: só cria quando as tabelas estão vazias. Em produção,
 * defina as variáveis de ambiente {@code APP_BOOTSTRAP_ADMIN_*} antes de subir.</p>
 *
 * <p>Cria, quando vazio:</p>
 * <ol>
 *   <li>As duas Organizations default ({@code TOP POWER ENGENHARIA} e
 *       {@code TOP POWER MATERIAIS}) com status ATIVO;</li>
 *   <li>O usuário ADMIN global ({@code ROLE_ADMIN}), que acessa todas as
 *       Organizations pelo role global — sem precisar de {@code UserOrganization};</li>
 *   <li>Um vínculo {@code UserOrganization} admin↔primeira org com
 *       {@code isDefault=true}, apenas para que haja uma Organization
 *       default pré-selecionada no login.</li>
 * </ol>
 */
@Component
@Order(0)
public class BootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminEmail;
    private final String adminPassword;
    private final boolean bootstrapEnabled;

    public BootstrapRunner(UserRepository userRepository,
                           OrganizationRepository organizationRepository,
                           UserOrganizationRepository userOrganizationRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap.admin.email:admin@toppower.com.br}") String adminEmail,
                           @Value("${app.bootstrap.admin.password:Admin@123}") String adminPassword,
                           @Value("${app.bootstrap.enabled:true}") boolean bootstrapEnabled) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.bootstrapEnabled = bootstrapEnabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!bootstrapEnabled) {
            log.info("Bootstrap desabilitado (app.bootstrap.enabled=false). Nada a fazer.");
            return;
        }

        // 1) Organizations padrão (se a tabela estiver vazia).
        if (organizationRepository.count() == 0) {
            log.warn("=================================================================");
            log.warn(" TABELA DE ORGANIZATIONS VAZIA: criando Organizations padrão.");
            log.warn("   -> TOP POWER ENGENHARIA");
            log.warn("   -> TOP POWER MATERIAIS");
            log.warn("=================================================================");

            Organization engenharia = new Organization();
            engenharia.setCorporateName("TOP POWER ENGENHARIA LTDA ME");
            engenharia.setTradeName("TOP POWER ENGENHARIA");
            engenharia.setCnpj("13.433.616/0001-06");
            engenharia.setStateRegistration("671.137.811.110");
            engenharia.setMunicipalRegistration("29764.01-6");
            engenharia.setZipCode("13170-700");
            engenharia.setStreet("AVENIDA REBOUCAS");
            engenharia.setNumber("4465");
            engenharia.setDistrict("RES. VECCON");
            engenharia.setCity("SUMARE");
            engenharia.setState("SP");
            engenharia.setStatus(OrganizationStatus.ATIVO);
            engenharia.setProposalPrefix("PT");
            organizationRepository.save(engenharia);

            Organization materiais = new Organization();
            materiais.setCorporateName("TOP POWER MATERIAIS LTDA ME");
            materiais.setTradeName("TOP POWER MATERIAIS");
            materiais.setCnpj("59.530.698/0001-08");
            materiais.setStateRegistration("671.700.534.116");
            materiais.setMunicipalRegistration("62965010");
            materiais.setZipCode("13171-456");
            materiais.setStreet("RUA JOAO RAVAGNANI");
            materiais.setNumber("36");
            materiais.setDistrict("JARDIM RESIDENCIAL RAVAGNANI");
            materiais.setCity("SUMARE");
            materiais.setState("SP");
            materiais.setStatus(OrganizationStatus.ATIVO);
            materiais.setProposalPrefix("PL");
            organizationRepository.save(materiais);

            log.info("Organizations padrão criadas (2).");
        } else {
            log.info("Tabela 'organizations' já possui registros. Bootstrap de Organizations não necessário.");
        }

        // 2) ADMIN default (se a tabela de usuários estiver vazia).
        if (userRepository.count() > 0) {
            log.info("Tabela 'users' já possui registros. Bootstrap de admin não necessário.");
            return;
        }

        log.warn("=================================================================");
        log.warn(" TABELA DE USUÁRIOS VAZIA: criando administrador padrão.");
        log.warn("   E-mail:  {}", adminEmail);
        log.warn("   Senha:   {}", adminPassword);
        log.warn("   -> Defina APP_BOOTSTRAP_ADMIN_PASSWORD=<outra-senha> no .env antes de subir para produção.");
        log.warn("=================================================================");

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);

        // 3) Vínculo default admin↔primeira Organization (para pré-selecionar
        //    a org ativa no login). O ADMIN acessa todas as orgs pelo role
        //    global; este vínculo existe apenas para satisfazer a default.
        organizationRepository.findAll().stream().findFirst().ifPresent(firstOrg -> {
            if (!userOrganizationRepository.existsByUserIdAndOrganizationId(admin.getId(), firstOrg.getId())) {
                UserOrganization link = new UserOrganization();
                link.setUser(admin);
                link.setOrganization(firstOrg);
                link.setRole(Role.ROLE_ADMIN);
                link.setDefault(true);
                userOrganizationRepository.save(link);
                log.info("Vínculo default admin↔Organization criado para '{}'.", firstOrg.getTradeName());
            }
        });

        log.info("Administrador padrão criado: {}", adminEmail);
    }
}