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
 */
public record UserDetailsImpl(UUID uuid, String email, String password, String role) implements UserDetails {

    public static UserDetailsImpl from(User user) {
        return new UserDetailsImpl(user.getUuid(), user.getEmail(), user.getPassword(), user.getRole().name());
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
