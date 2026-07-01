package br.com.toppower.erp_toppower.common.validation;

import br.com.toppower.erp_toppower.common.util.DocumentValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

/**
 * Validador de cross-field para {@link ValidTaxId}.
 *
 * <p>Acessa a classe do DTO por reflection para obter o documento fiscal
 * e o tipo de pessoa, e valida conforme o tipo.</p>
 */
public class TaxIdValidator implements ConstraintValidator<ValidTaxId, Object> {

    private String taxIdFieldName;
    private String personTypeFieldName;

    @Override
    public void initialize(ValidTaxId annotation) {
        this.taxIdFieldName = annotation.taxIdField();
        this.personTypeFieldName = annotation.personTypeField();
    }

    @Override
    public boolean isValid(Object dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        try {
            String taxId = readField(dto, taxIdFieldName, String.class);
            Object personType = readField(dto, personTypeFieldName, Object.class);

            // null/blank -> deixa @NotBlank/@NotNull validar presença
            if (taxId == null || taxId.isBlank()) {
                return true;
            }
            if (personType == null) {
                return true;
            }

            // Chama o validador correto baseado no enum PersonType (ou similar)
            return validateByType(taxId, personType);
        } catch (Exception e) {
            // Falha na reflection -> fail-safe (não valida)
            return true;
        }
    }

    /**
     * Lê um campo da classe do DTO, buscando também nas superclasses.
     */
    private <T> T readField(Object dto, String fieldName, Class<T> expectedType) throws Exception {
        Class<?> current = dto.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(dto);
                if (value == null) {
                    return null;
                }
                return expectedType.cast(value);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**
     * Valida o documento fiscal conforme o tipo.
     * Usa comparação por nome para evitar dependência forte com o enum
     * PersonType (do módulo client) — funciona com qualquer enum que tenha
     * valores "FISICA" e "JURIDICA".
     */
    private boolean validateByType(String taxId, Object personType) {
        String typeName = personType.toString();
        if ("FISICA".equals(typeName)) {
            return DocumentValidator.isValidCpf(taxId);
        } else if ("JURIDICA".equals(typeName)) {
            return DocumentValidator.isValidCnpj(taxId);
        }
        // Tipo desconhecido -> não valida (deixa o service layer tratar)
        return true;
    }
}
