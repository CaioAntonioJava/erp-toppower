package br.com.toppower.erp_toppower.common.entity;

import br.com.toppower.erp_toppower.common.listener.TenantEntityListener;
import br.com.toppower.erp_toppower.common.listener.UpperCaseFieldListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * Mapped superclass para todas as entidades <b>tenant-scoped</b>: dados de
 * negócio pertencentes a uma empresa (tenant) específica e que devem ser
 * isolados entre tenants.
 *
 * <p>Herda de {@link BaseEntity} (UUID + auditoria) e adiciona:</p>
 * <ul>
 *   <li>Coluna {@code tenant_uuid} — discrimina a qual tenant o registro pertence.
 *       Sem FK física (convenção do projeto: referências são por UUID).</li>
 *   <li>{@link FilterDef}/{@link Filter} Hibernate "tenantFilter" — aplica
 *       automaticamente {@code WHERE tenant_uuid = :tenantUuid} em toda query
 *       quando habilitado pelo {@code TenantFilterAspect}.</li>
 *   <li>{@link TenantEntityListener} — seta {@code tenant_uuid} no persist
 *       a partir do {@code TenantContext} (claim do JWT).</li>
 * </ul>
 *
 * <p><b>Não herdam desta classe</b> (permanecem globais):</p>
 * <ul>
 *   <li>{@code Tenant} — a própria empresa-dona;</li>
 *   <li>{@code UserTenant} — join N:N usuário↔tenant;</li>
 *   <li>{@code User} — usuário é global, o vínculo com tenant é via {@code UserTenant};</li>
 *   <li>{@code Cep} — dado de referência público, compartilhado entre tenants.</li>
 * </ul>
 */
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantUuid", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_uuid = :tenantUuid")
@EntityListeners({AuditingEntityListener.class, UpperCaseFieldListener.class, TenantEntityListener.class})
@Getter
@Setter
@NoArgsConstructor
public abstract class TenantScopedEntity extends BaseEntity {

    /**
     * Identificador (UUID) do tenant (empresa-dona) a que pertence o registro.
     *
     * <p>Populado automaticamente pelo {@link TenantEntityListener} no persist,
     * a partir do {@code TenantContext} (claim {@code tenant} do JWT). Setado
     * manualmente apenas em bootstrap/seed (quando não há requisição autenticada).</p>
     *
     * <p>Armazenado como {@code BINARY(16)} no MySQL (padrão do projeto para UUIDs).</p>
     */
    @Column(name = "tenant_uuid", nullable = false, updatable = false)
    private UUID tenantUuid;
}