package br.com.toppower.erp_toppower.config;

import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Fornece ao Spring Data JPA o e-mail do usuário atualmente autenticado,
 * para popular os campos {@code @CreatedBy} e {@code @LastModifiedBy} das entidades.
 *
 * Retorna {@link Optional#empty()} quando não ha usuário autenticado (por exemplo,
 * durante o bootstrap do primeiro usuário administrador antes do login).
 * Isso faz com que os campos de auditoria permaneam nulos, sem quebrar a aplicação.
 */
@Configuration
public class AuditorAwareImpl {

    @Bean(name = "auditorAware")
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetailsImpl userDetails) {
                return Optional.ofNullable(userDetails.email());
            }
            return Optional.empty();
        };
    }
}
