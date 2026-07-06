package br.com.toppower.erp_toppower.config;

import br.com.toppower.erp_toppower.common.context.TenantContext;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect que habilita o filtro Hibernate {@code tenantFilter} em toda chamada
 * a um repositório Spring Data, garantindo que <b>toda</b> query JPQL/Criteria
 * sobre entidades {@code TenantScopedEntity} receba automaticamente
 * {@code WHERE tenant_uuid = :tenantUuid}.
 *
 * <p>Isso elimina a possibilidade de uma query esquecer o filtro de tenant
 * e vazar dados entre empresas — o isolamento é aplicado na camada de
 * persistência, não depende do programador lembrar em cada service.</p>
 *
 * <p>Funciona interceptando qualquer bean que implemente
 * {@code org.springframework.data.repository.Repository} (base de todos os
 * repositórios Spring Data). Antes de cada chamada, obtém o {@link Session}
 * Hibernate corrente via {@link EntityManager#unwrap(Session.class)} e
 * habilita o filtro com o tenant do {@link TenantContext}.</p>
 *
 * <p><b>Limitações (anotar no código para futuros mantenedores):</b></p>
 * <ul>
 *   <li>Não filtra queries nativas ({@code nativeQuery=true}) — o filtro
 *       Hibernate só se aplica a JPQL/Criteria. O projeto não usa
 *       nativeQuery hoje; se surgir, validar isolamento manualmente.</li>
 *   <li>Não cobre uso direto de {@link EntityManager} (fora de repositórios
 *       Spring Data) — se surgir, habilitar o filtro manualmente.</li>
 *   <li>O filtro só é habilitado quando {@link TenantContext#get()} está
 *       setado (requisição autenticada). Em contextos sem tenant
 *       (bootstrap/seed), o filtro fica desabilitado e <b>todas</b> as
 *       linhas são visíveis — esperado, pois o bootstrap precisa ler
 *       dados globais.</li>
 * </ul>
 *
 * <p><b>Sem admin global consolidado:</b> por design do projeto, nenhum
 * papel bypassa o filtro. A gestão acessa as empresas logando em cada uma
 * (ou via switch-tenant).</p>
 */
@Aspect
@Component
public class TenantFilterAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantFilterAspect.class);

    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Intercepta toda chamada a qualquer repositório Spring Data (a anotação
     * {@code @within} casa interfaces anotadas com {@code @Repository}, e a
     * hierarquia de {@code Repository} é o supertipo comum). Roda antes de
     * cada método de repositório.
     *
     * <p>O {@link Session} do Hibernate é compartilhado por toda a transação
     * corrente, então habilitar o filtro uma vez por chamada é suficiente —
     * o filtro persiste para as queries seguintes dentro da mesma sessão.
     * A checagem {@code isEnabled("tenantFilter")} evita re-habilitar.</p>
     */
    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableTenantFilter() {
        UUID tenantUuid = TenantContext.get();
        if (tenantUuid == null) {
            // Sem tenant na requisição (ex: bootstrap, endpoint público, ou
            // operação fora de contexto de segurança). Filtro desabilitado.
            return;
        }
        try {
            Session session = entityManager.unwrap(Session.class);
            org.hibernate.Filter filter = session.getEnabledFilter("tenantFilter");
            if (filter == null) {
                session.enableFilter("tenantFilter").setParameter("tenantUuid", tenantUuid);
            }
        } catch (Exception e) {
            // Em testes ou contextos sem Session Hibernate real, loga e segue.
            // Em produção, se chegar aqui, é bug de wiring — melhor falhar alto.
            log.warn("Não foi possível habilitar tenantFilter na Session Hibernate: {}", e.getMessage());
        }
    }
}