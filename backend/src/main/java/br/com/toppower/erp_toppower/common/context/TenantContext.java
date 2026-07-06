package br.com.toppower.erp_toppower.common.context;

import java.util.UUID;

/**
 * Contexto de tenant da requisição corrente, baseado em {@link ThreadLocal}.
 *
 * <p>Populado pelo {@code JwtAuthenticationFilter} a partir do claim {@code tenant}
 * do JWT e limpo ao fim da requisição. Lido por:</p>
 * <ul>
 *   <li>{@code TenantEntityListener} — para setar {@code tenant_uuid} no persist</li>
 *   <li>{@code TenantFilterAspect} — para habilitar o {@code @Filter} do Hibernate</li>
 * </ul>
 *
 * <p>Seguindo o mesmo espírito do {@code AuditorAwareImpl}, que lê o principal
 * do {@code SecurityContextHolder}. Aqui mantemos um {@link ThreadLocal} próprio
 * para desacoplar do Spring Security e permitir leitura fora do contexto de
 * segurança (ex: dentro de listeners JPA, onde o SecurityContext pode não
 * estar disponível em threads de flush).</p>
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantUuid) {
        CURRENT.set(tenantUuid);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    /**
     * Retorna o tenant corrente ou lança {@link IllegalStateException} se ausente.
     * Útil em pontos onde o tenant é obrigatório (persist de entidade tenant-scoped).
     */
    public static UUID require() {
        UUID uuid = CURRENT.get();
        if (uuid == null) {
            throw new IllegalStateException(
                    "TenantContext não definido: não há tenant na requisição corrente. "
                            + "Isso indica que a operação está rodando fora de uma requisição "
                            + "autenticada (ex: bootstrap/seed) — forneça o tenant explicitamente.");
        }
        return uuid;
    }

    public static void clear() {
        CURRENT.remove();
    }
}