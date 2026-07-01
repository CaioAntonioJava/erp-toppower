package br.com.toppower.erp_toppower.common.util;

import java.util.regex.Pattern;

/**
 * Utilitário para geração de códigos sequenciais no formato {@code PREFIXNNNNNN}
 * (ex.: {@code EMP000001}, {@code CLI000042}).
 *
 * <p>Usado para popular automaticamente o campo {@code code} (código interno)
 * de entidades como {@code Company} e {@code Customer}. O caller é responsável
 * por consultar o maior código existente com o prefixo (ex.: via
 * {@code SELECT MAX(c.code) FROM Company c WHERE c.code LIKE 'EMP%'}) e
 * passar o resultado para {@link #nextCode(String, String, int)}.</p>
 *
 * <h2>Concorrência</h2>
 * <p>Este utilitário <b>não</b> é thread-safe por si só. A garantia de
 * unicidade vem da constraint {@code UNIQUE} na coluna {@code code} da
 * entidade, que provoca {@code DataIntegrityViolationException} caso duas
 * transações gerem o mesmo código simultaneamente. Em caso de colisão
 * durante {@code persist}, o caller deve repetir a operação (típico em
 * sistemas com baixa concorrência de cadastro — caso deste ERP).</p>
 */
public final class CodeSequenceGenerator {

    /**
     * Quantidade de dígitos do sufixo numérico.
     * Com 6 dígitos o sistema suporta até 999.999 registros por prefixo.
     */
    public static final int DEFAULT_PADDING_WIDTH = 6;

    /**
     * Regex que valida o formato {@code ^PREFIX[0-9]+$}.
     * O caller pode usar para validar códigos pré-existentes antes do parse.
     */
    private static final Pattern NUMERIC_SUFFIX_PATTERN = Pattern.compile("^[0-9]+$");

    private CodeSequenceGenerator() {
    }

    /**
     * Calcula o próximo código sequencial a partir do maior código existente
     * (filtrado pelo prefixo).
     *
     * <ul>
     *   <li>Se {@code maxCode} for {@code null}/{@code blank} (sem registros) →
     *       retorna {@code PREFIX + "0".repeat(width) + "1"} (ex.: {@code EMP000001}).</li>
     *   <li>Caso contrário, extrai o sufixo numérico, incrementa em 1 e
     *       re-completa com zeros à esquerda até {@code width} dígitos.</li>
     * </ul>
     *
     * @param maxCode maior código existente com o prefixo, ou {@code null} se
     *                ainda não houver registros
     * @param prefix  prefixo literal que identifica a entidade (ex.: {@code "EMP"});
     *                deve ter o mesmo tamanho usado na consulta ao banco
     * @param width   largura do sufixo numérico em dígitos (use
     *                {@link #DEFAULT_PADDING_WIDTH} para o padrão de 6 dígitos)
     * @return próximo código no formato {@code PREFIX + zeros à esquerda + sequência}
     * @throws NumberFormatException se {@code maxCode} não tiver um sufixo numérico válido
     */
    public static String nextCode(String maxCode, String prefix, int width) {
        long next = 1L;
        if (maxCode != null && !maxCode.isBlank()) {
            String suffix = maxCode.substring(prefix.length());
            if (!NUMERIC_SUFFIX_PATTERN.matcher(suffix).matches()) {
                throw new NumberFormatException(
                        "Código existente não possui sufixo numérico válido: " + maxCode);
            }
            next = Long.parseLong(suffix) + 1L;
        }
        return prefix + String.format("%0" + width + "d", next);
    }
}
