package br.com.toppower.erp_toppower.common.listener;

import br.com.toppower.erp_toppower.common.context.OrganizationContext;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity listener JPA que injeta automaticamente o {@code organization_uuid}
 * da entidade a partir do {@link OrganizationContext} no momento do persist.
 *
 * <p>Disparado em {@link jakarta.persistence.PrePersist}. Lê a Organization
 * corrente do {@link OrganizationContext} (populado pelo
 * {@code OrganizationContextFilter} a partir do header {@code X-Organization-Id})
 * e seta no campo {@code organizationUuid} da entidade.</p>
 *
 * <p>Segue o mesmo padrão de reflection cache do {@link UpperCaseFieldListener}:
 * o campo {@code organizationUuid} é resolvido por classe e cacheado em um
 * {@link ConcurrentHashMap}.</p>
 *
 * <p>Não sobrescreve valor já setado explicitamente pelo chamador (permite
 * que o bootstrap/seed definam a Organization manualmente quando não há
 * {@link OrganizationContext}).</p>
 */
public class OrganizationEntityListener {

    /** Cache: Class -> campo {@code organizationUuid} (null se a classe não for organization-scoped). */
    private static final Map<Class<?>, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    @jakarta.persistence.PrePersist
    public void onPrePersist(Object entity) {
        if (entity == null) {
            return;
        }
        Field orgField = resolveOrganizationField(entity.getClass());
        if (orgField == null) {
            // Entidade não é organization-scoped (ex: Organization, UserOrganization, Cep) — nada a fazer.
            return;
        }
        try {
            orgField.setAccessible(true);
            if (orgField.get(entity) != null) {
                // Já setado explicitamente pelo chamador — respeita.
                return;
            }
            UUID organizationUuid = OrganizationContext.get();
            if (organizationUuid == null) {
                // Sem contexto de Organization (bootstrap/seed). Nesse caso o
                // chamador deve ter setado o campo manualmente; se não o fez,
                // a coluna fica NULL e o Hibernate filter a exclui das queries
                // scoped — comportamento desejado (não vaza para nenhuma org).
                return;
            }
            orgField.set(entity, organizationUuid);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Não foi possível setar o campo 'organizationUuid' da classe "
                            + entity.getClass().getName(), e);
        }
    }

    /**
     * Resolve o campo {@code organizationUuid} na hierarquia da classe (declarado
     * em {@code OrganizationScopedEntity}). Retorna {@code null} se a classe não
     * herdar de {@code OrganizationScopedEntity}.
     */
    private static Field resolveOrganizationField(Class<?> entityClass) {
        return FIELD_CACHE.computeIfAbsent(entityClass, clazz -> {
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if ("organizationUuid".equals(field.getName())) {
                        return field;
                    }
                }
                current = current.getSuperclass();
            }
            return null;
        });
    }
}