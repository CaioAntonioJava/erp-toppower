package br.com.toppower.erp_toppower.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que uma String é um CPF válido (com dígitos verificadores).
 *
 * <p>Aceita CPF com ou sem formatação (pontos e traço). Retorna válido
 * se o valor for {@code null} (use {@code @NotBlank} para validar presença).</p>
 *
 * <p>Exemplo de uso:</p>
 * <pre>
 * &#064;NotBlank
 * &#064;ValidCpf
 * private String cpf;
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CpfValidator.class)
public @interface ValidCpf {

    String message() default "CPF inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
