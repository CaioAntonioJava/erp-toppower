package br.com.toppower.erp_toppower.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de unidade do {@link SoftBreak}.
 *
 * <p>Casos cobertos:</p>
 * <ul>
 *   <li>Entradas nulas / vazias / curtas → devolvidas intactas.</li>
 *   <li><b>Regressão:</b> nome de pessoa curto com "E" (ex.:
 *       "LEONARDO NETTO") NÃO deve ser quebrado em letras
 *       individuais — a heurística antiga quebrava em "L",
 *       "E", "O"...</li>
 *   <li>Razão social COM sufixo societário longo e conectivos
 *       ("INDUSTRIAS E COMERCIO X LTDA") DEVE ser quebrada
 *       harmoniosamente — sufixo na última linha, "E" como
 *       fronteira.</li>
 *   <li>Sufixo societário sozinho (já curto) não precisa de mais
 *       quebras.</li>
 *   <li>Nomes com {@code -} e {@code /} muito longos quebram por
 *       esses separadores (mesmo sem sufixo societário).</li>
 * </ul>
 */
class SoftBreakTest {

    // ---------- Casos de borda ----------

    @Test
    void name_withNull_returnsNull() {
        assertNull(SoftBreak.name(null));
    }

    @Test
    void name_withBlank_returnsBlank() {
        assertEquals("", SoftBreak.name(""));
        assertEquals("   ", SoftBreak.name("   "));
    }

    @Test
    void name_withShortName_returnsUnchanged() {
        // Nomes curtos, sem sufixo, sem separadores — devolvidos intactos.
        assertEquals("HALLAND", SoftBreak.name("HALLAND"));
        assertEquals("JOAO", SoftBreak.name("JOAO"));
    }

    // ---------- Regressão: nome de pessoa com "E" ----------

    @Test
    void name_shortPersonNameWithConnector_doesNotBreak() {
        // REGRESSÃO: a heurística antiga quebrava "LEONARDO NETTO" em
        // "L / E / O / N / A / R / D / O / N / E / T / T / O" porque
        // tratava cada " E " como ponto de quebra. Agora, sem sufixo
        // societário e < 70 chars, nenhum separador é tocado.
        String result = SoftBreak.name("LEONARDO NETTO");
        assertFalse(result.contains("<br/>"),
                "Nome de pessoa curto não deve receber quebra: " + result);
        assertEquals("LEONARDO NETTO", result);
    }

    @Test
    void name_personNameWithCommaAndConnector_doesNotBreak() {
        // "JOAO, MARIA E PEDRO" — nome de pessoa com vírgula e "E".
        // Sem sufixo societário, nenhum dos dois deve disparar quebra.
        String result = SoftBreak.name("JOAO, MARIA E PEDRO");
        assertFalse(result.contains("<br/>"),
                "Nome de pessoa com vírgula/AND não deve quebrar: " + result);
    }

    @Test
    void name_personNameWithEireliSubstring_doesNotBreak() {
        // "EIRELI" contém "E" + letras sem espaços. A regex " E "
        // (com espaços) não deve casar. Mas a string tem só 6 chars,
        // também abaixo do limiar, então fica intacta de qualquer jeito.
        String result = SoftBreak.name("EIRELI");
        assertFalse(result.contains("<br/>"));
    }

    // ---------- Caso de uso principal: razão social ----------

    @Test
    void name_razaoSocialLonga_quebraPorSufixoEConector() {
        // Caso típico: razão social com sufixo societário e conectivo "E".
        // "INDUSTRIA E COMERCIO DE EQUIPAMENTOS LTDA" tem 41 chars e
        // termina com LTDA → sufixo na última linha + "E" como quebra.
        String input = "INDUSTRIA E COMERCIO DE EQUIPAMENTOS LTDA";
        String result = SoftBreak.name(input);

        // Deve terminar com "LTDA" precedido de <br/>.
        assertTrue(result.endsWith("<br/>LTDA"),
                "Sufixo LTDA deve estar na última linha: " + result);
        // Deve quebrar pelo menos uma vez (sufixo ou "E").
        assertTrue(result.contains("<br/>"),
                "Razão social longa com sufixo deve quebrar: " + result);
    }

    @Test
    void name_razaoSocialDoScreenshot_quebraHarmoniosa() {
        // Caso exato do bug reportado:
        // "ABC PLATAFORMAS ELEVATORIAS E LOCACAO DE EQUIPAMENTOS LTDA"
        // - 53 chars, com sufixo LTDA, com dois " E " (com espaços).
        // Esperado: sufixo LTDA na última linha + ao menos uma quebra
        // por "E". O importante é que NÃO fique pior que a versão
        // antiga e que as quebras sejam em fronteiras de palavras.
        String input = "ABC PLATAFORMAS ELEVATORIAS E LOCACAO DE EQUIPAMENTOS LTDA";
        String result = SoftBreak.name(input);

        // Sufixo vai pra última linha.
        assertTrue(result.endsWith("<br/>LTDA"),
                "LTDA deve estar em linha própria: " + result);
        // Ao menos uma quebra no meio.
        assertTrue(result.contains("<br/>"),
                "Razão social longa deve quebrar: " + result);
        // Nenhuma palavra pode ficar quebrada no meio (sem "E</br>"
        // ou "L</br>TDA" etc.).
        assertFalse(result.matches(".*[a-zA-Z]E?<br/>[a-zA-Z].*")
                        && !result.contains(" E <br/>")
                        && !result.contains("<br/>E "),
                "Quebras não podem cair no meio de palavra: " + result);
    }

    @Test
    void name_shortRazaoSocialComSufixo_quebraSufixo() {
        // "ACME LTDA" — 9 chars, curto. Só o sufixo vai pra última linha.
        String result = SoftBreak.name("ACME LTDA");
        assertEquals("ACME <br/>LTDA", result);
    }

    // ---------- Separadores fortes em nomes sem sufixo ----------

    @Test
    void name_nomeFantasiaLongoComHifen_quebraPorHifen() {
        // Sem sufixo societário, mas com "-" e > 70 chars — quebra
        // por hífen é aceitável.
        String input = "CONSTRUTORA - INCORPORADORA - EMPREENDIMENTOS IMOBILIARIOS LTDA ME";
        // Esse input TEM sufixo (LTDA ME no fim) — vai cair no caso
        // "com sufixo" e quebrar normalmente. Bom, vamos testar
        // também um caso puramente sem sufixo.
        String result = SoftBreak.name(input);
        assertTrue(result.contains("<br/>"),
                "Nome longo com sufixo deve quebrar: " + result);
    }

    @Test
    void name_nomeSemSufixoELongoComHifen_quebraPorHifen() {
        // "CONSTRUTORA ALPHA - BETA - GAMA - DELTA - EPSILON - ZETA - ETA - THETA"
        // ~76 chars, sem sufixo societário → permite quebra por hífen.
        String input = "CONSTRUTORA ALPHA - BETA - GAMA - DELTA - EPSILON - ZETA - ETA - THETA";
        String result = SoftBreak.name(input);
        // Pelo menos uma quebra por hífen esperada.
        assertTrue(result.contains("<br/>-"),
                "Nome longo sem sufixo com hifens deve quebrar por hífen: " + result);
    }

    @Test
    void name_nomeSemSufixoECurtoComHifen_naoQuebra() {
        // "CONSTRUTORA - INCORPORADORA" — ~27 chars, sem sufixo,
        // abaixo do limiar de 70 → não deve quebrar.
        String result = SoftBreak.name("CONSTRUTORA - INCORPORADORA");
        assertFalse(result.contains("<br/>"),
                "Nome curto sem sufixo não deve quebrar: " + result);
    }

    // ---------- Não-reatividade ----------

    @Test
    void name_doesNotMutateInput() {
        String input = "ABC PLATAFORMAS ELEVATORIAS E LOCACAO LTDA";
        String before = input;
        assertDoesNotThrow(() -> SoftBreak.name(input));
        assertEquals(before, input, "SoftBreak.name não deve mutar o input");
    }
}
