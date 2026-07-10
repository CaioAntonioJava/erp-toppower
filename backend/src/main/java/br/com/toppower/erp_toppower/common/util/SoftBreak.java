package br.com.toppower.erp_toppower.common.util;

/**
 * Utilitário para inserir quebras de linha "macias" (soft breaks) em
 * strings longas — tipicamente razão social / nome fantasia de clientes
 * — usadas na renderização de PDFs e relatórios.
 *
 * <p>O objetivo é puramente visual: quando um nome é grande demais
 * para caber em uma linha da coluna onde está sendo renderizado, ele
 * quebra de forma <b>harmoniosa</b> (em fronteiras semânticas) em vez
 * de quebrar no meio de uma cláusula jurídica.</p>
 *
 * <p>Estratégia:</p>
 * <ol>
 *   <li>Detecta sufixos societários brasileiros comuns
 *       ({@code LTDA}, {@code S.A.}, {@code ME}, {@code EPP},
 *       {@code EIRELI}, {@code S/S}, {@code LTDA.}, {@code S.A},
 *       {@code SA}, {@code MEI}, {@code EP}, {@code SCP},
 *       {@code SS}, {@code SLU}, {@code SPE}, etc.) <b>no final</b> da
 *       string e insere um {@code <br/>} imediatamente antes deles — o
 *       sufixo vai para a última linha, o nome principal fica nas
 *       linhas anteriores.</li>
 *   <li>Para nomes <b>com</b> sufixo societário e &gt; 60 chars,
 *       quebra também pelos conectivos "E" e vírgula — partindo do
 *       pressuposto de que é uma razão social longa (ex.:
 *       "INDUSTRIAS E COMERCIO X LTDA").</li>
 *   <li>Para nomes <b>sem</b> sufixo societário (nomes de pessoa,
 *       nomes fantasia curtos), não quebra por "E" / vírgula — esses
 *       caracteres aparecem em nomes próprios e a quebra polui a
 *       leitura. Só quebramos por {@code -} e {@code /} em nomes
 *       realmente enormes (&gt; 70 chars).</li>
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
     * Limite mínimo de caracteres para a heurística de separadores
     * internos "fortes" ({@code -}, {@code /}) em nomes SEM sufixo
     * societário. Nomes mais curtos que isso ficam intactos — isso
     * evita que nomes de pessoa como "LEONARDO NETTO" (14 chars)
     * recebam qualquer quebra.
     */
    private static final int MIN_LENGTH_FOR_INTERNAL_BREAKS_NO_SUFFIX = 70;

    /**
     * Limite mínimo de caracteres para a heurística de separadores
     * "E" e vírgula (conectivos) em nomes COM sufixo societário. É
     * menor que o de nomes sem sufixo porque a presença do sufixo
     * já indica que é uma razão social longa, e queremos quebrar
     * "INDUSTRIAS E COMERCIO X LTDA" em vez de manter tudo numa linha
     * que não cabe na coluna.
     */
    private static final int MIN_LENGTH_FOR_CONNECTOR_BREAKS_WITH_SUFFIX = 60;

    /**
     * Aplica quebras harmoniosas em uma string. Retorna a string
     * original se ela for nula, vazia, ou já curta o suficiente para
     * não precisar de tratamento.
     *
     * <p>Estratégia refinada (vs. versão anterior, que quebrava nomes
     * curtos como "LEONARDO NETTO" letra por letra):</p>
     * <ol>
     *   <li>Se o nome terminar com sufixo societário (LTDA, S.A., ME,
     *       EIRELI etc.) → vai pra última linha. Em seguida, se for
     *       longo o suficiente, quebra também pelos conectivos "E" /
     *       vírgulas. Esta é a heurística "agressiva", reservada a
     *       razões sociais.</li>
     *   <li>Caso contrário (nome de pessoa, nome fantasia curto) →
     *       nenhuma quebra por "E" / vírgula. Só quebramos por
     *       {@code -} e {@code /} em nomes realmente enormes
     *       (&gt; 70 chars), que é virtualmente impossível para um
     *       nome de pessoa mas acontece com nomes fantasia muito
     *       longos.</li>
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

        boolean hasTrailingSuffix = hasTrailingSociedadeSuffix(trimmed);

        // 1) Sufixo societário no final → vai pra última linha.
        String result = hasTrailingSuffix
                ? breakBeforeTrailingSuffix(trimmed)
                : trimmed;

        // 2) Separadores semânticos internos — só se o nome for grande
        //    o suficiente E (tiver sufixo OU for realmente enorme).
        boolean allowConnectors = hasTrailingSuffix
                && result.length() >= MIN_LENGTH_FOR_CONNECTOR_BREAKS_WITH_SUFFIX;
        boolean allowHardSeparators = hasTrailingSuffix
                ? result.length() >= MIN_LENGTH_FOR_CONNECTOR_BREAKS_WITH_SUFFIX
                : result.length() >= MIN_LENGTH_FOR_INTERNAL_BREAKS_NO_SUFFIX;

        if (allowHardSeparators) {
            result = breakBeforeInternalSeparators(result, allowConnectors);
        }

        return result;
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
