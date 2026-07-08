package br.com.toppower.erp_toppower.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Helpers de formatação monetária/percentual compartilhados entre a
 * camada de serviços (JSON) e os templates Thymeleaf (PDF).
 *
 * <p>Locale fixo em {@code pt_BR} para garantir consistência visual
 * entre o que o frontend mostra e o que o PDF imprime — caso contrário,
 * os dois lados poderiam divergir quando rodando em máquinas com
 * locale diferente.</p>
 *
 * <p>{@code null} e {@code ZERO} são tratados especialmente: para
 * moeda retorna {@code "—"} (em-dash, idêntico ao usado nas páginas
 * React atuais) e para percentual retorna {@code "0,00%"}.</p>
 */
public final class CurrencyFormatter {

    /** Locale fixo pt-BR; evita divergência entre app e PDF. */
    public static final Locale PT_BR = new Locale("pt", "BR");

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(PT_BR);
    private static final NumberFormat PERCENT = NumberFormat.getPercentInstance(PT_BR);

    static {
        // Percentual com 2 casas: 10% → "10,00%".
        PERCENT.setMinimumFractionDigits(2);
        PERCENT.setMaximumFractionDigits(2);
        // Currency já vem com 2 casas por default no JDK.
    }

    private CurrencyFormatter() {
    }

    /**
     * Formata um valor monetário em BRL.
     *
     * @param value valor em reais (pode ser nulo)
     * @return string no formato {@code R$ 1.234,56} ou {@code "—"} se
     *         o valor for nulo
     */
    public static String formatBRL(BigDecimal value) {
        if (value == null) return "—";
        // Garante 2 casas independente da precisão de entrada.
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        return CURRENCY.format(scaled);
    }

    /**
     * Formata um percentual a partir de um valor já em "pontos percentuais"
     * (ex.: {@code BigDecimal.TEN} → {@code "10,00%"}).
     *
     * <p>Se precisar formatar um valor já em base fracionária (ex.:
     * {@code 0.10} → {@code "10,00%"}), use {@link #formatFractionAsPercent}.</p>
     *
     * @param percent pontos percentuais (10 = 10%, não 0,10)
     * @return string formatada ou {@code "0,00%"} se nulo
     */
    public static String formatPercent(BigDecimal percent) {
        if (percent == null) return formatPercent(BigDecimal.ZERO);
        return PERCENT.format(percent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }

    /**
     * Formata uma fração como percentual (ex.: {@code 0.1} → {@code "10,00%"}).
     */
    public static String formatFractionAsPercent(BigDecimal fraction) {
        if (fraction == null) return formatPercent(BigDecimal.ZERO);
        return PERCENT.format(fraction);
    }

    /**
     * Formata uma data ISO curta ({@code yyyy-MM-dd}) em pt-BR.
     * Retorna {@code "—"} para entrada nula ou inválida.
     */
    public static String formatDate(java.time.LocalDate date) {
        if (date == null) return "—";
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR));
    }
}