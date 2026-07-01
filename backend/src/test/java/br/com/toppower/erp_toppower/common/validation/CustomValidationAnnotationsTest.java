package br.com.toppower.erp_toppower.common.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomValidationAnnotationsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void teardown() {
        factory.close();
    }

    // ========== @ValidCpf ==========

    static class CpfHolder {
        @ValidCpf
        String cpf;

        CpfHolder(String cpf) {
            this.cpf = cpf;
        }
    }

    @Test
    void cpfValido_aceito() {
        CpfHolder holder = new CpfHolder("123.456.789-09");
        Set<ConstraintViolation<CpfHolder>> violations = validator.validate(holder);
        assertTrue(violations.isEmpty());
    }

    @Test
    void cpfInvalido_dvErrado_rejeitado() {
        CpfHolder holder = new CpfHolder("123.456.789-00");
        Set<ConstraintViolation<CpfHolder>> violations = validator.validate(holder);
        assertEquals(1, violations.size());
        assertEquals("CPF inválido", violations.iterator().next().getMessage());
    }

    @Test
    void cpfNulo_aceito() {
        // null é aceito (use @NotBlank para validar presença)
        CpfHolder holder = new CpfHolder(null);
        Set<ConstraintViolation<CpfHolder>> violations = validator.validate(holder);
        assertTrue(violations.isEmpty());
    }

    // ========== @ValidCnpj ==========

    static class CnpjHolder {
        @ValidCnpj
        String cnpj;

        CnpjHolder(String cnpj) {
            this.cnpj = cnpj;
        }
    }

    @Test
    void cnpjValido_aceito() {
        CnpjHolder holder = new CnpjHolder("11.222.333/0001-81");
        Set<ConstraintViolation<CnpjHolder>> violations = validator.validate(holder);
        assertTrue(violations.isEmpty());
    }

    @Test
    void cnpjInvalido_dvErrado_rejeitado() {
        CnpjHolder holder = new CnpjHolder("11.222.333/0001-00");
        Set<ConstraintViolation<CnpjHolder>> violations = validator.validate(holder);
        assertEquals(1, violations.size());
        assertEquals("CNPJ inválido", violations.iterator().next().getMessage());
    }
}
