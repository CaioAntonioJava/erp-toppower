package br.com.toppower.erp_toppower.config;

import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect que habilita o filtro Hibernate {@code organizationFilter} em toda
 * chamada a um repositório Spring Data, garantindo que <b>toda</b> query
 * JPQL/Criteria sobre entidades {@code OrganizationScopedEntity} receba
 * automaticamente {@code WHERE organization_uuid = :organizationUuid}.
 *
 * <p>Isso elimina a possibilidade de uma query esquecer o filtro de
 * Organization e vazar dados entre empresas — o isolamento é aplicado na
 * camada de persistência, não depende do programador lembrar em cada service.</p>
 *
 * <p>Funciona interceptando qualquer bean que implemente
 * {@code org.springframework.data.repository.Repository} (base de todos os
 * repositórios Spring Data). Antes de cada chamada, obtém o {@link Session}
 * Hibernate corrente via {@link EntityManager#unwrap(Session.class)} e
 * habilita o filtro com a Organization do {@link OrganizationContext}.</p>
 *
 * <p><b>Limitações (anotar no código para futuros mantenedores):</b></p>
 * <ul>
 *   <li>Não filtra queries nativas ({@code nativeQuery=true}) — o filtro
 *       Hibernate só se aplica a JPQL/Criteria. O projeto não usa
 *       nativeQuery hoje; se surgir, validar isolamento manualmente.</li>
 *   <li>Não cobre uso direto de {@link EntityManager} (fora de repositórios
 *       Spring Data) — se surgir, habilitar o filtro manualmente.</li>
 *   <li>O filtro só é habilitado quando {@link OrganizationContext#get()} está
 *       setado (requisição autenticada com header {@code X-Organization-Id}).
 *       Em contextos sem Organization (bootstrap/seed, admin sem header ativo),
 *       o filtro fica desabilitado e <b>todas</b> as linhas são visíveis.</li>
 * </ul>
 */
@Aspect
@Component
public class OrganizationFilterAspect {

    private static final Logger log = LoggerFactory.getLogger(OrganizationFilterAspect.class);

    private final EntityManager entityManager;

    public OrganizationFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Intercepta toda chamada a qualquer repositório Spring Data. Roda antes
     * de cada método de repositório.
     *
     * <p>O {@link Session} do Hibernate é compartilhado por toda a transação
     * corrente, então habilitar o filtro uma vez por chamada é suficiente —
     * o filtro persiste para as queries seguintes dentro da mesma sessão.
     * A checagem {@code getEnabledFilter(...)} evita re-habilitar.</p>
     */
    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableOrganizationFilter() {
        UUID organizationUuid = OrganizationContext.get();
        if (organizationUuid == null) {
            // Sem Organization na requisição (ex: bootstrap, endpoint público,
            // admin sem header ativo). Filtro desabilitado — vê tudo.
            return;
        }
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("organizationFilter") == null) {
                session.enableFilter("organizationFilter").setParameter("organizationUuid", organizationUuid);
            }
        } catch (Exception e) {
            // Em testes ou contextos sem Session Hibernate real, loga e segue.
            log.warn("Não foi possível habilitar organizationFilter na Session Hibernate: {}", e.getMessage());
        }
    }
}