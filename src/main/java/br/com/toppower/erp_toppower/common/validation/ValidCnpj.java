package br.com.toppower.erp_toppower.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que uma String é um CNPJ válido (com dígitos verificadores).
 *
 * <p>Aceita CNPJ com ou sem formatação (pontos, barra e traço). Retorna
 * válido se o valor for {@code null} (use {@code @NotBlank} para validar presença).</p>
 *
 * <p>Exemplo de uso:</p>
 * <pre>
 * &#064;NotBlank
 * &#064;ValidCnpj
 * private String cnpj;
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CnpjValidator.class)
public @interface ValidCnpj {

    String message() default "CNPJ inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
