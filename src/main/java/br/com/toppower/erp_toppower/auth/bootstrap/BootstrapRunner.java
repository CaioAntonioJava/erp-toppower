package br.com.toppower.erp_toppower.auth.bootstrap;

import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * CREDENCIAIS INICIAIS. Use somente na primeira subida da aplicação.
     * Em produção, sobrescreva via variáveis de ambiente (APP_BOOTSTRAP_ADMIN_*) no .env.
     */
    private final String adminEmail;
    private final String adminPassword;
    private final boolean bootstrapEnabled;

    public BootstrapRunner(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap.admin.email:admin@toppower.com.br}") String adminEmail,
                           @Value("${app.bootstrap.admin.password:Admin@123}") String adminPassword,
                           @Value("${app.bootstrap.enabled:true}") boolean bootstrapEnabled) {
        this.userRepository = userRepository;
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
        if (userRepository.count() > 0) {
            log.info("Tabela 'users' já possui registros. Bootstrap não necessário.");
            return;
        }

        log.warn("=================================================================");
        log.warn(" TABELA DE USUÁRIOS VAZIA: criando administrador padrão.");
        log.warn("   E-mail:  {}", adminEmail);
        log.warn("   Senha:   {}", adminPassword);
        log.warn("   -> Use esta credencial para o primeiro login.");
        log.warn("   -> Defina APP_BOOTSTRAP_ADMIN_PASSWORD=<outra-senha> no .env antes de subir para produção.");
        log.warn("=================================================================");

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);

        log.info("Administrador padrão criado: {}", adminEmail);
    }
}
