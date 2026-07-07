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

import java.util.UUID;

/**
 * Mapped superclass para todas as entidades <b>organization-scoped</b>: dados
 * de negócio pertencentes a uma Organization (empresa) específica e que devem
 * ser isolados entre empresas.
 *
 * <p>Herda de {@link BaseEntity} (UUID + auditoria) e adiciona:</p>
 * <ul>
 *   <li>Coluna {@code organization_uuid} — discrimina a qual Organization o
 *       registro pertence. Sem FK física (convenção do projeto: referências
 *       são por UUID).</li>
 *   <li>{@link FilterDef}/{@link Filter} Hibernate "organizationFilter" —
 *       aplica automaticamente {@code WHERE organization_uuid = :organizationUuid}
 *       em toda query JPQL/Criteria quando habilitado pelo
 *       {@code OrganizationFilterAspect}.</li>
 *   <li>{@link OrganizationEntityListener} — seta {@code organization_uuid}
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
@FilterDef(name = "organizationFilter", parameters = @ParamDef(name = "organizationUuid", type = UUID.class))
@Filter(name = "organizationFilter", condition = "organization_uuid = :organizationUuid")
@EntityListeners({AuditingEntityListener.class, UpperCaseFieldListener.class, OrganizationEntityListener.class})
@Getter
@Setter
@NoArgsConstructor
public abstract class OrganizationScopedEntity extends BaseEntity {

    /**
     * Identificador (UUID) da Organization a que pertence o registro.
     *
     * <p>Populado automaticamente pelo {@link OrganizationEntityListener} no
     * persist, a partir do {@code OrganizationContext} (header
     * {@code X-Organization-Id} da requisição). Setado manualmente apenas em
     * bootstrap/seed (quando não há requisição autenticada).</p>
     *
     * <p>Armazenado como {@code BINARY(16)} no MySQL (padrão do projeto para
     * UUIDs). A coluna é nullable para tolerar rows legadas; o listener
     * garante preenchimento em novos inserts.</p>
     */
    @Column(name = "organization_uuid", updatable = false)
    private UUID organizationUuid;
}