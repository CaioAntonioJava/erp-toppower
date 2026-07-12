package br.com.toppower.erp_toppower.common.util;

/**
 * Utilitário para inserir quebras de linha "macias" (soft breaks) em
 * strings longas — tipicamente razão social / nome fantasia de clientes
 * — usadas na renderização de PDFs e relatórios.
 *
 * <p>O objetivo é puramente visual: <b>uma única linha é sempre
 * preferida</b>; a quebra explícita via {@code <br/>} é usada apenas
 * como último recurso, quando o nome é longo o suficiente para que
 * a quebra natural do renderer produza resultado ruim (palavra órfã
 * na última linha, sufixo societário isolado na linha 3, etc.).</p>
 *
     * <p>O renderer do PDF (OpenHTMLtoPDF) já faz wrap natural em
     * fronteiras de palavra, e o template limita o bloco a 2 linhas via
     * CSS ({@code max-height: 2.8em; overflow: hidden}). O painel do
     * cliente agora ocupa a largura total da página, então nomes
     * comuns cabem em 1 linha. Insere-se {@code <br/>} <b>só</b> em
     * nomes muito longos (≥ 80 chars), para ancorar o sufixo
     * societário na última linha visível.</p>
 *
 * <p>Estratégia:</p>
 * <ol>
 *   <li>Nomes curtos/médios (até o limiar {@link #LONG_NAME_THRESHOLD})
 *       → devolvidos intactos. O renderer quebra naturalmente em
 *       1 linha (caso típico) ou 2 linhas (caso marginal). Sem
 *       {@code <br/>} algum.</li>
 *   <li>Nomes longos COM sufixo societário ({@code LTDA},
 *       {@code S.A.}, {@code ME}, ...) → insere {@code <br/>} antes
 *       do sufixo para que "LTDA" fique na última linha visível, em
 *       vez de ser jogado para a linha 3 (que seria cortada pela
 *       regra CSS de 2 linhas).</li>
 *   <li>Nomes longos SEM sufixo societário e com separadores fortes
 *       ({@code -}, {@code /}, {@code &}) → quebra por esses
 *       separadores para evitar word-break no meio de palavra.</li>
 * </ol>
 *
 * <p>Esta classe é deliberadamente pequena e sem dependências para
 * poder ser usada em código Java. Para uso em templates Thymeleaf,
 * chame-a no service que monta o modelo e exponha o resultado como
 * uma chave do {@code Map<String, Object>} — expressões Thymeleaf em
 * SpEL restrito (padrão do Spring Boot) não permitem
 * {@code T(SomeClass).method(...)}.</p>
 */
public final class SoftBreak {

    private SoftBreak() {
        // utilitário estático
    }

    /**
     * Sufixos societários brasileiros (maiúsculos, com ou sem ponto).
     * Ordenados por tamanho decrescente para garantir que a regex
     * casa o mais específico primeiro (ex.: {@code S.A.} antes de
     * {@code SA}).
     */
    private static final String[] SOCIEDADE_SUFFIXES = {
            "LTDA.", "LTDA", "EIRELI", "S.A.", "S/A", "S.A", "SA",
            "ME.", "ME", "EPP", "EP", "MEI", "SCP", "S/S", "SS",
            "SLU", "SPE", "INC.", "INC", "LLC", "CORP."
    };

    /**
     * Limite mínimo de caracteres a partir do qual a string é
     * considerada "longa" e justificaria a inserção de {@code <br/>}.
     *
     * <p>O painel do cliente agora ocupa a largura total da página
     * útil do A4 (~17cm), onde uma linha a 7pt comporta ~145
     * caracteres. Ajustamos o limiar em 80 chars para ficar bem
     * abaixo da capacidade de 1 linha, garantindo que nomes médios
     * sejam deixados para o wrap natural — uma linha quase sempre
     * é suficiente. A quebra explícita só entra em nomes
     * genuinamente longos (razões sociais com 80+ chars).</p>
     */
    private static final int LONG_NAME_THRESHOLD = 80;

    /**
     * Aplica quebras harmoniosas em uma string. Retorna a string
     * original se ela for nula, vazia, ou curta o suficiente para
     * caber naturalmente em até 2 linhas no painel do PDF (caso
     * comum — wrap natural do renderer já resolve).
     *
     * <p>Estratégia (vs. versão anterior, que quebrava
     * <b>sempre</b> antes do sufixo societário):</p>
     * <ol>
     *   <li>Nomes até {@link #LONG_NAME_THRESHOLD} chars → ficam
     *       intactos. O renderer quebra em 1 ou 2 linhas conforme a
     *       largura disponível; nenhuma intervenção manual.</li>
     *   <li>Nomes longos COM sufixo societário (LTDA, S.A., ME,
     *       EIRELI etc.) → {@code <br/>} antes do sufixo, ancorando-o
     *       na última linha visível (a regra CSS limita o bloco a
     *       2 linhas).</li>
     *   <li>Nomes longos SEM sufixo societário mas com separadores
     *       "fortes" ({@code -}, {@code /}, {@code &}) → quebra por
     *       esses separadores.</li>
     * </ol>
     *
     * @param name nome a ser quebrado (pode ser {@code null})
     * @return string com {@code <br/>} inseridos, ou a entrada original
     */
    public static String name(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String trimmed = name.trim();

        // Nomes curtos/médios: zero intervenção. O renderer quebra
        // naturalmente em até 2 linhas (regra CSS) e a primeira
        // opção é sempre 1 linha.
        if (trimmed.length() < LONG_NAME_THRESHOLD) {
            return trimmed;
        }

        boolean hasTrailingSuffix = hasTrailingSociedadeSuffix(trimmed);

        // Nome longo COM sufixo societário → ancorar o sufixo na
        // última linha visível para que ele não vire palavra órfã
        // numa eventual linha 3 (cortada pelo max-height).
        if (hasTrailingSuffix) {
            return breakBeforeTrailingSuffix(trimmed);
        }

        // Nome longo SEM sufixo, com separadores fortes → quebrar
        // neles para evitar word-break no meio de palavra.
        return breakBeforeInternalSeparators(trimmed, false);
    }

    /**
     * Detecta (sem alterar) se a string termina com algum dos
     * sufixos societários conhecidos. Usado para escolher entre a
     * heurística agressiva (com sufixo) e a conservadora (sem).
     */
    private static boolean hasTrailingSociedadeSuffix(String input) {
        for (String suffix : SOCIEDADE_SUFFIXES) {
            if (input.endsWith(" " + suffix) || input.endsWith(suffix)) {
                int idx = input.length() - suffix.length();
                if (idx > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String breakBeforeTrailingSuffix(String input) {
        for (String suffix : SOCIEDADE_SUFFIXES) {
            // Se o input termina com " " + suffix (espaço antes), separamos
            // com espaço. Se termina com suffix colado (sem espaço), não.
            boolean spaceSeparated = input.endsWith(" " + suffix);
            if (spaceSeparated || input.endsWith(suffix)) {
                int idx = input.length() - suffix.length();
                // Garante que existe algum conteúdo antes do sufixo.
                if (idx > 0) {
                    String prefix = input.substring(0, idx).trim();
                    String joiner = spaceSeparated ? " " : "";
                    return prefix + joiner + "<br/>" + suffix;
                }
            }
        }
        return input;
    }

    private static String breakBeforeInternalSeparators(String input, boolean allowConnectors) {
        String result = input;

        // 2a) Traços e barras (sempre quebram antes, se o nome for grande).
        result = result.replaceAll("\\s+-\\s+", "<br/>- ");
        result = result.replaceAll("\\s+/\\s+", "<br/>/ ");
        result = result.replaceAll("\\s+&\\s+", "<br/>& ");

        // 2b) Vírgulas e conectivo "E" — só para nomes com sufixo
        // societário (razão social) e longos. Em nome de pessoa
        // ("JOAO, MARIA E PEDRO") jamais queremos quebrar.
        if (allowConnectors) {
            result = result.replaceAll(",\\s+", ",<br/>");
            // " E " (com espaços) no meio: quebra antes. Cuidado para
            // não quebrar "EIRELI" (que contém "E" + letras, sem espaço).
            result = result.replaceAll(" E ", "<br/>E ");
        }

        return result;
    }
}
