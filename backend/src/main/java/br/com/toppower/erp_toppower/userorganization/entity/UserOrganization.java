package br.com.toppower.erp_toppower.userorganization.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Relacionamento N:N entre {@link User} e {@link Organization}.
 *
 * <p>Representa o acesso de um usuário a uma Organization específica, com:</p>
 * <ul>
 *   <li>{@code role} — papel de negócio do usuário NAQUELA Organization
 *       (coexiste com {@code User.role}, que é o papel global de acesso
 *       ao sistema). Usuários {@code ROLE_ADMIN} globais acessam todas as
 *       Organizations automaticamente, sem precisar de vínculo aqui.</li>
 *   <li>{@code isDefault} — Organization usada por padrão após o login.
 *       Apenas uma linha por usuário deve ter {@code isDefault = true}.</li>
 * </ul>
 *
 * <p><b>Global</b>: não herda de {@code OrganizationScopedEntity} — é metadado
 * de acesso, não dado de negócio.</p>
 */
@Entity
@Table(name = "user_organizations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_org",
                columnNames = {"user_uuid", "organization_uuid"}))
@Getter
@Setter
@NoArgsConstructor
public class UserOrganization extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "user_uuid", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "organization_uuid", nullable = false)
    private Organization organization;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 25)
    private Role role;
}