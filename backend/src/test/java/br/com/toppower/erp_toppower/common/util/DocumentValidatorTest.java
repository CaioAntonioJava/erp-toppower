package br.com.toppower.erp_toppower.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentValidatorTest {

    // ========== CPF valido ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "123.456.789-09",
            "111.444.777-35",
            "529.982.247-25"
    })
    void cpfFormatadoValido(String cpf) {
        assertTrue(DocumentValidator.isValidCpf(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678909",
            "11144477735",
            "52998224725"
    })
    void cpfSemFormatacaoValido(String cpf) {
        assertTrue(DocumentValidator.isValidCpf(cpf));
    }

    // ========== CPF invalido ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "123.456.789-00",
            "123.456.789-10",
            "111.444.777-00",
            "11144477700"
    })
    void cpfComDvErrado(String cpf) {
        assertFalse(DocumentValidator.isValidCpf(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "111.111.111-11",
            "222.222.222-22",
            "00000000000",
            "99999999999"
    })
    void cpfSequenciaRepetida(String cpf) {
        assertFalse(DocumentValidator.isValidCpf(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "123",
            "123.456.789",
            "123.456.789-0",
            "123.456.789-0123"
    })
    void cpfTamanhoIncorreto(String cpf) {
        assertFalse(DocumentValidator.isValidCpf(cpf));
    }

    @Test
    void cpfNulo() {
        assertFalse(DocumentValidator.isValidCpf(null));
    }

    // ========== CNPJ valido ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "11.222.333/0001-81",
            "11.444.777/0001-61"
    })
    void cnpjFormatadoValido(String cnpj) {
        assertTrue(DocumentValidator.isValidCnpj(cnpj));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11222333000181",
            "11444777000161"
    })
    void cnpjSemFormatacaoValido(String cnpj) {
        assertTrue(DocumentValidator.isValidCnpj(cnpj));
    }

    // ========== CNPJ invalido ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "11.222.333/0001-00",
            "11.222.333/0001-82",
            "11.444.777/0001-00",
            "11222333000100"
    })
    void cnpjComDvErrado(String cnpj) {
        assertFalse(DocumentValidator.isValidCnpj(cnpj));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11.111.111/1111-11",
            "22.222.222/2222-22",
            "00000000000000",
            "99999999999999"
    })
    void cnpjSequenciaRepetida(String cnpj) {
        assertFalse(DocumentValidator.isValidCnpj(cnpj));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "11.222.333",
            "11.222.333/0001",
            "11.222.333/0001-811"
    })
    void cnpjTamanhoIncorreto(String cnpj) {
        assertFalse(DocumentValidator.isValidCnpj(cnpj));
    }

    @Test
    void cnpjNulo() {
        assertFalse(DocumentValidator.isValidCnpj(null));
    }
}
