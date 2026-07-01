package br.com.toppower.erp_toppower.common.listener;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity listener JPA que normaliza para MAIÚSCULAS todos os campos
 * {@code String} anotados com {@link UpperCase}.
 *
 * <p>Disparado em {@link PrePersist} e {@link PreUpdate}, então a
 * transformação acontece no momento da gravação, independente do
 * caminho de entrada (DTO/controller, repository.save direto, seed,
 * migration, fixtures, etc.).</p>
 *
 * <p>Percorre a hierarquia de classes da entidade (a anotação
 * {@code @UpperCase} pode estar em campos declarados na própria
 * classe, numa {@code @MappedSuperclass} como {@code BaseEntity} ou
 * {@code BasePerson}, etc.).</p>
 *
 * <p>Performance: a lista de campos anotados é resolvida por classe
 * e cacheada em um {@link ConcurrentHashMap} (TTL infinito — a
 * definição de classes não muda em runtime).</p>
 *
 * <p>Usa {@link Locale#ROOT} para evitar surpresas de i18n (em turco,
 * {@code "i".toUpperCase()} vira {@code "İ"} com ponto acima; com
 * {@code ROOT} vira {@code "I"}).</p>
 */
public class UpperCaseFieldListener {

    /** Cache: Class -> lista de campos anotados com @UpperCase (imutável). */
    private static final Map<Class<?>, List<Field>> FIELDS_CACHE = new ConcurrentHashMap<>();

    @PrePersist
    public void onPrePersist(Object entity) {
        normalize(entity);
    }

    @PreUpdate
    public void onPreUpdate(Object entity) {
        normalize(entity);
    }

    private void normalize(Object entity) {
        if (entity == null) {
            return;
        }
        for (Field field : getUpperCaseFields(entity.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value instanceof String str && !str.isEmpty()) {
                    field.set(entity, str.toUpperCase(Locale.ROOT));
                }
            } catch (IllegalAccessException e) {
                // Se o campo for final sem bypass, aborta a operação com mensagem clara.
                throw new IllegalStateException(
                        "Não foi possível normalizar o campo '" + field.getName()
                                + "' da classe " + entity.getClass().getName()
                                + " para MAIÚSCULAS. Verifique se o campo não é final/static.",
                        e);
            }
        }
    }

    /**
     * Retorna todos os campos {@code String} anotados com {@link UpperCase}
     * declarados na classe e em todas as suas superclasses (até {@code Object}).
     * O resultado é cacheado por classe.
     */
    private static List<Field> getUpperCaseFields(Class<?> entityClass) {
        return FIELDS_CACHE.computeIfAbsent(entityClass, UpperCaseFieldListener::collectFields);
    }

    private static List<Field> collectFields(Class<?> entityClass) {
        List<Field> result = new ArrayList<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                if (field.getType() != String.class) {
                    continue;
                }
                if (field.isAnnotationPresent(UpperCase.class)) {
                    result.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return Collections.unmodifiableList(result);
    }
}
