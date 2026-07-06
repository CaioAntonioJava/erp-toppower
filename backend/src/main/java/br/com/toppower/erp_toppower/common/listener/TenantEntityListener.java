package br.com.toppower.erp_toppower.common.listener;

import br.com.toppower.erp_toppower.common.context.TenantContext;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity listener JPA que injeta automaticamente o {@code tenant_uuid} da
 * entidade a partir do {@link TenantContext} no momento do persist.
 *
 * <p>Disparado em {@link jakarta.persistence.PrePersist}. Lê o tenant
 * corrente do {@link TenantContext} (populado pelo {@code JwtAuthenticationFilter}
 * a partir do claim do JWT) e seta no campo {@code tenantUuid} da entidade.</p>
 *
 * <p>Segue o mesmo padrão de reflection cache do {@link UpperCaseFieldListener}:
 * o campo {@code tenantUuid} é resolvido por classe e cacheado em um
 * {@link ConcurrentHashMap}.</p>
 *
 * <p>Não sobrescreve valor já setado explicitamente pelo chamador (permite
 * que o bootstrap/seed definam o tenant manualmente quando não há
 * {@link TenantContext}).</p>
 */
public class TenantEntityListener {

    /** Cache: Class -> campo {@code tenantUuid} (pode ser null se a classe não for tenant-scoped). */
    private static final Map<Class<?>, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    @jakarta.persistence.PrePersist
    public void onPrePersist(Object entity) {
        if (entity == null) {
            return;
        }
        Field tenantField = resolveTenantField(entity.getClass());
        if (tenantField == null) {
            // Entidade não é tenant-scoped (ex: Tenant, UserTenant, Cep) — nada a fazer.
            return;
        }
        try {
            tenantField.setAccessible(true);
            if (tenantField.get(entity) != null) {
                // Já setado explicitamente pelo chamador — respeita.
                return;
            }
            UUID tenantUuid = TenantContext.get();
            if (tenantUuid == null) {
                // Sem contexto de tenant (bootstrap/seed). Nesse caso o chamador deve
                // ter setado o campo manualmente; se não o fez, a constraint NOT NULL
                // da coluna vai rejeitar no flush — comportamento desejado (fail fast).
                return;
            }
            tenantField.set(entity, tenantUuid);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Não foi possível setar o campo 'tenantUuid' da classe "
                            + entity.getClass().getName(), e);
        }
    }

    /**
     * Resolve o campo {@code tenantUuid} na hierarquia da classe (declarado
     * em {@code TenantScopedEntity}). Retorna {@code null} se a classe não
     * herdar de {@code TenantScopedEntity}.
     */
    private static Field resolveTenantField(Class<?> entityClass) {
        return FIELD_CACHE.computeIfAbsent(entityClass, clazz -> {
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if ("tenantUuid".equals(field.getName())) {
                        return field;
                    }
                }
                current = current.getSuperclass();
            }
            return null;
        });
    }
}