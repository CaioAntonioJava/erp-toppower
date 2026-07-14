package br.com.toppower.erp_toppower.common.entity;

import br.com.toppower.erp_toppower.common.listener.OrganizationEntityListener;
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

/**
 * Mapped superclass para todas as entidades <b>organization-scoped</b>: dados
 * de negócio pertencentes a uma Organization (empresa) específica e que devem
 * ser isolados entre empresas.
 *
 * <p>Herda de {@link BaseEntity} (Long + auditoria) e adiciona:</p>
 * <ul>
 *   <li>Coluna {@code organization_id} — discrimina a qual Organization o
 *       registro pertence. Sem FK física (convenção do projeto: referências
 *       são por ID numérico).</li>
 *   <li>{@link FilterDef}/{@link Filter} Hibernate "organizationFilter" —
 *       aplica automaticamente {@code WHERE organization_id = :organizationId}
 *       em toda query JPQL/Criteria quando habilitado pelo
 *       {@code OrganizationFilterAspect}.</li>
 *   <li>{@link OrganizationEntityListener} — seta {@code organization_id}
 *       no persist a partir do {@code OrganizationContext} (header
 *       {@code X-Organization-Id} da requisição).</li>
 * </ul>
 *
 * <p><b>Não herdam desta classe</b> (permanecem globais):</p>
 * <ul>
 *   <li>{@code Organization} — a própria empresa;</li>
 *   <li>{@code UserOrganization} — join N:N usuário↔org;</li>
 *   <li>{@code User} — usuário é global, o vínculo com org é via
 *       {@code UserOrganization} (ou pelo role global {@code ROLE_ADMIN},
 *       que acessa todas as orgs);</li>
 *   <li>{@code Profile} — perfil é do usuário, não da empresa;</li>
 *   <li>{@code Cep} — dado de referência público, compartilhado.</li>
 * </ul>
 */
@MappedSuperclass
@FilterDef(name = "organizationFilter", parameters = @ParamDef(name = "organizationId", type = Long.class))
@Filter(name = "organizationFilter", condition = "organization_id = :organizationId")
@EntityListeners({AuditingEntityListener.class, UpperCaseFieldListener.class, OrganizationEntityListener.class})
@Getter
@Setter
@NoArgsConstructor
public abstract class OrganizationScopedEntity extends BaseEntity {

    /**
     * Identificador (Long) da Organization a que pertence o registro.
     *
     * <p>Populado automaticamente pelo {@link OrganizationEntityListener} no
     * persist, a partir do {@code OrganizationContext} (header
     * {@code X-Organization-Id} da requisição). Setado manualmente apenas em
     * bootstrap/seed (quando não há requisição autenticada).</p>
     *
     * <p>A coluna é nullable para tolerar rows legadas; o listener
     * garante preenchimento em novos inserts.</p>
     */
    @Column(name = "organization_id", updatable = false)
    private Long organizationId;
}