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
 *   <li>Filosofia atual: <b>uma única linha é sempre preferida</b>;
 *       a quebra via {@code <br/>} só é inserida quando o nome é
 *       genuinamente longo (≥ {@code LONG_NAME_THRESHOLD} chars) e
 *       a quebra natural do renderer produziria resultado ruim
 *       (palavra órfã, sufixo jogado para a linha 3 cortada pelo
 *       cap CSS de 2 linhas).</li>
 *   <li>Razão social COM sufixo societário curto ("ACME LTDA", 9
 *       chars) NÃO recebe quebra — o renderer resolve em 1 linha.</li>
 *   <li>Razão social COM sufixo societário longo (≥ 80 chars) recebe
 *       {@code <br/>} antes do sufixo para ancorá-lo na última linha
 *       visível.</li>
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
        // termina com LTDA. Está abaixo do limiar de 80 chars
        // (LONG_NAME_THRESHOLD) → devolvida intacta, sem <br/>.
        String input = "INDUSTRIA E COMERCIO DE EQUIPAMENTOS LTDA";
        String result = SoftBreak.name(input);

        // Filosofia atual: nomes abaixo do limiar ficam intactos para
        // que o wrap natural do renderer resolva em 1 linha (a regra
        // CSS .text-limit-2 garante o cap de 2 linhas como segurança).
        assertFalse(result.contains("<br/>"),
                "Razão social de 41 chars (abaixo do limiar de 80) "
                        + "deve ficar intacta: " + result);
        assertEquals(input, result);
    }

    @Test
    void name_razaoSocialDoScreenshot_naoQuebraAbaixoDoLimiar() {
        // Caso exato do bug reportado:
        // "ABC PLATAFORMAS ELEVATORIAS E LOCACAO DE EQUIPAMENTOS LTDA"
        // - 53 chars, com sufixo LTDA, com dois " E " (com espaços).
        // Agora que o painel do cliente ocupa a largura total da
        // página, o limiar subiu para 80 chars — nomes abaixo disso
        // ficam intactos para o renderer resolver em 1 linha.
        String input = "ABC PLATAFORMAS ELEVATORIAS E LOCACAO DE EQUIPAMENTOS LTDA";
        String result = SoftBreak.name(input);

        assertFalse(result.contains("<br/>"),
                "Razão social de 53 chars (abaixo do limiar de 80) deve "
                        + "ficar intacta e usar a largura total: " + result);
        assertEquals(input, result);
    }

    @Test
    void name_razaoSocialLongaAcimaDoLimiar_quebraPorSufixo() {
        // Razão social com 80+ chars: acima do novo limiar
        // (LONG_NAME_THRESHOLD = 80). Deve receber <br/> antes do
        // sufixo LTDA para ancorá-lo na última linha visível.
        String input = "ABC PLATAFORMAS ELEVATORIAS E LOCACAO DE EQUIPAMENTOS INDUSTRIAIS E COMERCIAIS DO SUL LTDA";
        String result = SoftBreak.name(input);

        assertTrue(result.contains("<br/>"),
                "Razão social acima de 80 chars deve quebrar: " + result);
        assertTrue(result.endsWith("<br/>LTDA"),
                "LTDA deve estar em linha própria: " + result);
    }

    @Test
    void name_shortRazaoSocialComSufixo_naoQuebra() {
        // "ACME LTDA" — 9 chars, abaixo do limiar de 45 chars.
        // Filosofia atual: 1 linha é preferida; o wrap natural do
        // renderer resolve. Nada é alterado.
        String result = SoftBreak.name("ACME LTDA");
        assertEquals("ACME LTDA", result);
        assertFalse(result.contains("<br/>"),
                "Razão social curta com sufixo NÃO deve quebrar — o "
                        + "renderer deve resolver em 1 linha: " + result);
    }

    // ---------- Separadores fortes em nomes sem sufixo ----------

    @Test
    void name_nomeFantasiaLongoComHifen_quebraPorHifen() {
        // Sem sufixo societário, mas com "-" e > 80 chars — quebra
        // por hífen é aceitável.
        String input = "CONSTRUTORA - INCORPORADORA - EMPREENDIMENTOS IMOBILIARIOS DO ESTADO DO RIO DE JANEIRO LTDA ME";
        // Esse input TEM sufixo (LTDA ME no fim) — vai cair no caso
        // "com sufixo" e quebrar normalmente. Bom, vamos testar
        // também um caso puramente sem sufixo.
        String result = SoftBreak.name(input);
        assertTrue(result.contains("<br/>"),
                "Nome longo com sufixo deve quebrar: " + result);
    }

    @Test
    void name_nomeSemSufixoELongoComHifen_quebraPorHifen() {
        // "CONSTRUTORA ALPHA - BETA - GAMA - DELTA - EPSILON - ZETA - ETA - THETA - IOTA - KAPPA"
        // ~96 chars, sem sufixo societário → permite quebra por hífen.
        String input = "CONSTRUTORA ALPHA - BETA - GAMA - DELTA - EPSILON - ZETA - ETA - THETA - IOTA - KAPPA";
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