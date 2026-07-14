package br.com.toppower.erp_toppower.common.context;

/**
 * Contexto de Organization da requisição corrente, baseado em {@link ThreadLocal}.
 *
 * <p>Populado pelo {@code OrganizationContextFilter} a partir do header HTTP
 * {@code X-Organization-Id} (após validar acesso) e limpo ao fim da requisição.
 * Lido por:</p>
 * <ul>
 *   <li>{@code OrganizationEntityListener} — para setar
 *       {@code organization_id} no persist;</li>
 *   <li>{@code OrganizationFilterAspect} — para habilitar o
 *       {@code @Filter} do Hibernate e escopar as queries.</li>
 * </ul>
 *
 * <p>Seguindo o mesmo espírito do {@code AuditorAwareImpl}, que lê o principal
 * do {@code SecurityContextHolder}. Aqui mantemos um {@link ThreadLocal} próprio
 * para desacoplar do Spring Security e permitir leitura fora do contexto de
 * segurança (ex: dentro de listeners JPA, em threads de flush).</p>
 */
public final class OrganizationContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private OrganizationContext() {
    }

    public static void set(Long organizationId) {
        CURRENT.set(organizationId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    /**
     * Retorna a Organization corrente ou lança {@link IllegalStateException}
     * se ausente. Útil em pontos onde a Organization é obrigatória (persist
     * de entidade organization-scoped).
     */
    public static Long require() {
        Long id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException(
                    "OrganizationContext não definido: não há Organization na requisição "
                            + "corrente. Isso indica que a operação está rodando fora de uma "
                            + "requisição autenticada (ex: bootstrap/seed) — forneça a "
                            + "Organization explicitamente.");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}