package br.com.toppower.erp_toppower.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validação cross-field para documento fiscal (CPF ou CNPJ).
 *
 * <p>Deve ser aplicada no nível da CLASSE, não do campo. O validador
 * procura dois campos na classe:</p>
 * <ul>
 *   <li>{@code taxIdField} (default: {@code "taxId"}) — String com o documento</li>
 *   <li>{@code personTypeField} (default: {@code "personType"}) — enum com o tipo</li>
 * </ul>
 *
 * <p>Validação aplicada conforme o tipo:</p>
 * <ul>
 *   <li>{@code FISICA} → valida como CPF (11 dígitos + DV)</li>
 *   <li>{@code JURIDICA} → valida como CNPJ (14 dígitos + DV)</li>
 * </ul>
 *
 * <p>Se algum dos campos for {@code null} (ou vazio para taxId), a validação
 * passa — use {@code @NotNull}/{@code @NotBlank} nos campos individuais
 * para validar presença.</p>
 *
 * <h2>Exemplo de uso:</h2>
 * <pre>
 * &#064;ValidTaxId(taxIdField = "taxId", personTypeField = "personType")
 * public record ClientCreateRequest(
 *     &#064;NotBlank String taxId,
 *     &#064;NotNull PersonType personType,
 *     // ... outros campos
 * ) {}
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TaxIdValidator.class)
public @interface ValidTaxId {

    String message() default "CPF/CNPJ inválido (dígitos verificadores incorretos ou formato incorreto)";

    /**
     * Nome do campo que contém o documento (CPF/CNPJ).
     * Default: {@code "taxId"}.
     */
    String taxIdField() default "taxId";

    /**
     * Nome do campo que contém o enum do tipo de pessoa.
     * Default: {@code "personType"}.
     */
    String personTypeField() default "personType";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
