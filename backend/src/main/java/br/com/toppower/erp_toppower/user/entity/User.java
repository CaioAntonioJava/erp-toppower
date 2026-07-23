package br.com.toppower.erp_toppower.user.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import br.com.toppower.erp_toppower.user.enums.Module;
import br.com.toppower.erp_toppower.user.enums.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 200)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 25)
    private Role role = Role.ROLE_MANAGER;

    /**
     * Módulos (paineis) aos quais o usuário tem acesso. Relevantes apenas
     * para {@link Role#ROLE_EMPLOYEE}; usuários {@code ROLE_ADMIN} e
     * {@code ROLE_MANAGER} recebem todos os módulos automaticamente.
     *
     * <p>A tabela {@code user_permissions} é criada/gerenciada pelo Hibernate
     * (via {@code @ElementCollection} + {@code ddl-auto=update}).</p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "module", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<Module> modules = new HashSet<>();
}
