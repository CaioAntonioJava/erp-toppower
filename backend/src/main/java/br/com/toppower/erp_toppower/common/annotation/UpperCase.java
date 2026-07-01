package br.com.toppower.erp_toppower.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marcador de campo {@code String} que deve ser normalizado para
 * MAIÚSCULAS antes de ser persistido/atualizado no banco.
 *
 * <p>O processamento é feito pelo
 * {@link br.com.toppower.erp_toppower.common.listener.UpperCaseFieldListener},
 * que percorre a hierarquia de classes da entidade, encontra todos os
 * campos {@code String} anotados com {@code @UpperCase} e aplica
 * {@code toUpperCase()} (com trim implícito em valores nulos/blank).</p>
 *
 * <p>Regra aplicada: <b>no momento da gravação</b>, o valor do campo
 * será sempre MAIÚSCULAS, independente de como chegou à entidade
 * (DTO, repository.save direto, seed, migration, etc.).</p>
 *
 * <p>Exemplo de uso:</p>
 * <pre>
 * &#64;UpperCase
 * &#64;Column(name = "name", nullable = false, length = 150)
 * private String name;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpperCase {
}
