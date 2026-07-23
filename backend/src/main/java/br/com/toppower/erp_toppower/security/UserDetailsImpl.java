package br.com.toppower.erp_toppower.security;

import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.enums.Module;
import br.com.toppower.erp_toppower.user.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter que expõe a entidade {@link User} para o Spring Security,
 * mantendo a entidade JPA desacoplada de {@link UserDetails}.
 *
 * <p>As authorities emitidas combinam o papel global ({@code ROLE_*}) e os
 * módulos (paineis) acessíveis ({@code MODULE_*}). Usuários
 * {@code ROLE_ADMIN} e {@code ROLE_MANAGER} recebem todos os módulos;
 * {@code ROLE_EMPLOYEE} recebe apenas os módulos concedidos.</p>
 */
public record UserDetailsImpl(Long id, String email, String password, String role,
                               Set<Module> modules) implements UserDetails {

    public static UserDetailsImpl from(User user) {
        return new UserDetailsImpl(user.getId(), user.getEmail(), user.getPassword(),
                user.getRole().name(), effectiveModules(user));
    }

    /**
     * Módulos efetivamente acessíveis: todos para ADMIN/MANAGER, apenas os
     * concedidos para EMPLOYEE.
     */
    static Set<Module> effectiveModules(User user) {
        if (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_MANAGER) {
            return EnumSet.allOf(Module.class);
        }
        return user.getModules() == null ? Set.of() : EnumSet.copyOf(user.getModules());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));
        for (Module module : modules) {
            authorities.add(new SimpleGrantedAuthority(module.name()));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // O Spring Security usa o username como identificador principal da autenticação;
        // neste projeto o login é feito por e-mail.
        return email;
    }

    /**
     * Indica se o usuário autenticado possui o papel de administrador.
     * Útil para checagens de autorização em regras que liberam acesso total ao ADMIN.
     */
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(role);
    }

    /**
     * Indica se o usuário autenticado possui o papel de gestor.
     */
    public boolean isManager() {
        return "ROLE_MANAGER".equals(role);
    }
}