package br.com.toppower.erp_toppower.user.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Vínculo N:N entre {@link User} e {@code Tenant}.
 *
 * <p>Representa quais tenants (empresas operadoras) um usuário pode acessar.
 * Um usuário pode pertencer a múltiplos tenants e alternar entre eles via
 * {@code POST /auth/switch-tenant}.</p>
 *
 * <p><b>Não herda de {@code TenantScopedEntity}</b> — esta tabela é o próprio
 * mapeamento de acesso, não um dado de negócio de um tenant. Herda de
 * {@link BaseEntity} para UUID + auditoria.</p>
 *
 * <p>Referências ({@code user_uuid}, {@code tenant_uuid}) são colunas UUID
 * <b>sem FK física</b> — convenção do projeto (relacionamentos lógicos, não
 * físicos). {@code tenant_uuid} aqui é livre (não filtrado pelo Hibernate
 * {@code @Filter}, já que a entidade não herda de {@code TenantScopedEntity}).</p>
 */
@Entity
@Table(name = "user_tenants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_tenants_user_tenant",
                columnNames = {"user_uuid", "tenant_uuid"}))
@Getter
@Setter
@NoArgsConstructor
public class UserTenant extends BaseEntity {

    /**
     * UUID do usuário vinculado. Referência lógica (sem FK física) à tabela {@code users}.
     */
    @Column(name = "user_uuid", nullable = false, updatable = false)
    private UUID userUuid;

    /**
     * UUID do tenant vinculado. Referência lógica (sem FK física) à tabela {@code tenants}.
     */
    @Column(name = "tenant_uuid", nullable = false, updatable = false)
    private UUID tenantUuid;

    public UserTenant(UUID userUuid, UUID tenantUuid) {
        this.userUuid = userUuid;
        this.tenantUuid = tenantUuid;
    }
}