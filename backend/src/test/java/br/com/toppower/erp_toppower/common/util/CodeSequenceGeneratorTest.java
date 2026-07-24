package br.com.toppower.erp_toppower.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes unitários do {@link CodeSequenceGenerator}.
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>Geração do primeiro código (sem registros prévios)</li>
 *   <li>Incremento a partir de código existente</li>
 *   <li>Padding com largura customizada</li>
 *   <li>Exceção para sufixo não numérico</li>
 *   <li>Tratamento de maxCode em branco</li>
 * </ul>
 */
class CodeSequenceGeneratorTest {

    @Test
    void nextCode_semRegistros_retornaPrimeiroCodigo() {
        String result = CodeSequenceGenerator.nextCode(null, "EMP", 6);
        assertEquals("EMP000001", result);
    }

    @Test
    void nextCode_maxCodeEmBranco_retornaPrimeiroCodigo() {
        String result = CodeSequenceGenerator.nextCode("", "CLI", 6);
        assertEquals("CLI000001", result);
    }

    @Test
    void nextCode_maxCodeApenasEspacos_retornaPrimeiroCodigo() {
        String result = CodeSequenceGenerator.nextCode("   ", "CLI", 6);
        assertEquals("CLI000001", result);
    }

    @Test
    void nextCode_comRegistroExistente_incrementa() {
        String result = CodeSequenceGenerator.nextCode("EMP000001", "EMP", 6);
        assertEquals("EMP000002", result);
    }

    @Test
    void nextCode_comRegistroAlto_incrementaCorretamente() {
        String result = CodeSequenceGenerator.nextCode("CLI000042", "CLI", 6);
        assertEquals("CLI000043", result);
    }

    @Test
    void nextCode_comPaddingCustomizado_aplicaLargura() {
        String result = CodeSequenceGenerator.nextCode(null, "PROD", 4);
        assertEquals("PROD0001", result);
    }

    @Test
    void nextCode_comPaddingCustomizadoExistente_aplicaLargura() {
        String result = CodeSequenceGenerator.nextCode("PROD0001", "PROD", 4);
        assertEquals("PROD0002", result);
    }

    @Test
    void nextCode_sufixoNumericoGrande_incrementa() {
        String result = CodeSequenceGenerator.nextCode("NF999999", "NF", 6);
        assertEquals("NF1000000", result);
    }

    @Test
    void nextCode_sufixoInvalido_lancaNumberFormatException() {
        assertThrows(NumberFormatException.class,
                () -> CodeSequenceGenerator.nextCode("EMP_ABC", "EMP", 6));
    }

    @Test
    void nextCode_sufixoComLetras_lancaNumberFormatException() {
        assertThrows(NumberFormatException.class,
                () -> CodeSequenceGenerator.nextCode("EMP123A", "EMP", 6));
    }

    @Test
    void nextCode_prefixoDiferente_extraiSufixoCorretamente() {
        String result = CodeSequenceGenerator.nextCode("OR999", "OR", 3);
        assertEquals("OR1000", result);
    }
}
