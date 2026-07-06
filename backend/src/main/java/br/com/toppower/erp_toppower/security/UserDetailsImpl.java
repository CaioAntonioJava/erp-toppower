package br.com.toppower.erp_toppower.security;

import br.com.toppower.erp_toppower.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adapter que expõe a entidade {@link User} para o Spring Security,
 * mantendo a entidade JPA desacoplada de {@link UserDetails}.
 *
 * <p>Carrega também o {@code tenantUuid} da sessão corrente — o tenant
 * selecionado no login (ou via switch-tenant). Esse UUID é:</p>
 * <ul>
 *   <li>gravado no JWT como claim {@code tenant} por {@code JwtService};</li>
 *   <li>lido no filtro e populado no {@code TenantContext}, que habilita o
 *       filtro Hibernate {@code tenantFilter} e alimenta o
 *       {@code TenantEntityListener} no persist.</li>
 * </ul>
 */
public record UserDetailsImpl(UUID uuid, String email, String password, String role, UUID tenantUuid) implements UserDetails {

    public static UserDetailsImpl from(User user) {
        return new UserDetailsImpl(user.getUuid(), user.getEmail(), user.getPassword(),
                user.getRole().name(), null);
    }

    /**
     * Constroi o adapter com o tenant da sessão corrente (selecionado no
     * login ou no switch-tenant). O {@code tenantUuid} é o que será gravado
     * no JWT e usado para isolar as queries da sessão.
     */
    public static UserDetailsImpl from(User user, UUID tenantUuid) {
        return new UserDetailsImpl(user.getUuid(), user.getEmail(), user.getPassword(),
                user.getRole().name(), tenantUuid);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
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
}
