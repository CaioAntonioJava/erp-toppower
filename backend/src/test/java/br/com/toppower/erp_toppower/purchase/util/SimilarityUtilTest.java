package br.com.toppower.erp_toppower.purchase.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários do {@link SimilarityUtil}.
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>Normalização: lowercase, remoção de acentos, pontuação e espaços colapsados</li>
 *   <li>Similaridade: strings idênticas, diferentes, parcialmente similares</li>
 *   <li>Casos extremos: nulo, vazio, blank</li>
 * </ul>
 */
class SimilarityUtilTest {

    // ========== normalize ==========

    @Test
    void normalize_textoNulo_retornaVazio() {
        assertEquals("", SimilarityUtil.normalize(null));
    }

    @Test
    void normalize_textoVazio_retornaVazio() {
        assertEquals("", SimilarityUtil.normalize(""));
    }

    @Test
    void normalize_textoBlank_retornaVazio() {
        assertEquals("", SimilarityUtil.normalize("   "));
    }

    @Test
    void normalize_lowercaseESemAcentos() {
        assertEquals("joao silva", SimilarityUtil.normalize("João Silva"));
    }

    @Test
    void normalize_semPontuacao() {
        assertEquals("cabo de aco 1 4", SimilarityUtil.normalize("Cabo de Aço 1/4\""));
    }

    @Test
    void normalize_colapsaEspacosMultiplos() {
        assertEquals("cabo de aco", SimilarityUtil.normalize("Cabo   de   Aço"));
    }

    @Test
    void normalize_removeAcentosAcentuacao() {
        assertEquals("cafe", SimilarityUtil.normalize("café"));
    }

    @Test
    void normalize_caracteresEspeciais() {
        assertEquals("cabo 1 4 mm", SimilarityUtil.normalize("Cabo 1/4\" (mm)"));
    }

    @Test
    void normalize_textoJaNormalizado_inalterado() {
        assertEquals("cabo de aco", SimilarityUtil.normalize("cabo de aco"));
    }

    // ========== similarity ==========

    @Test
    void similarity_stringsIdenticas_retornaUm() {
        double result = SimilarityUtil.similarity("Cabo de Aço", "Cabo de Aço");
        assertEquals(1.0, result, 0.001);
    }

    @Test
    void similarity_stringsDiferentes_retornaBaixo() {
        double result = SimilarityUtil.similarity("Cabo de Aço", "Parafuso");
        assertTrue(result < 0.3);
    }

    @Test
    void similarity_ambasVazias_retornaUm() {
        assertEquals(1.0, SimilarityUtil.similarity("", ""), 0.001);
    }

    @Test
    void similarity_umaVazia_retornaZero() {
        assertEquals(0.0, SimilarityUtil.similarity("Cabo", ""), 0.001);
    }

    @Test
    void similarity_umaNula_retornaZero() {
        assertEquals(0.0, SimilarityUtil.similarity(null, "Cabo"), 0.001);
    }

    @Test
    void similarity_ambasNulas_retornaUm() {
        assertEquals(1.0, SimilarityUtil.similarity(null, null), 0.001);
    }

    @Test
    void similarity_comAcentuacaoDiferente_ignoraAcentos() {
        double result = SimilarityUtil.similarity("João Silva", "JOAO SILVA");
        assertEquals(1.0, result, 0.001);
    }

    @Test
    void similarity_parcialmenteSimilar() {
        double result = SimilarityUtil.similarity("Cabo de Aço 1/4", "Cabo de Aço 1/2");
        assertTrue(result > 0.5 && result < 1.0);
    }

    @Test
    void similarity_comPontuacao_ignoraPontuacao() {
        double result = SimilarityUtil.similarity("Cabo, de Aço!", "Cabo de Aço");
        assertEquals(1.0, result, 0.001);
    }
}
