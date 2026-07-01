package br.com.toppower.erp_toppower.common.validation;

import br.com.toppower.erp_toppower.common.util.DocumentValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null é considerado válido aqui; use @NotBlank para validar presença
        if (value == null) {
            return true;
        }
        return DocumentValidator.isValidCpf(value);
    }
}
