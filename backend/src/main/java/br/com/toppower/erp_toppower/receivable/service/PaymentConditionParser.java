package br.com.toppower.erp_toppower.receivable.service;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário estático que interpreta uma condição de pagamento e devolve
 * o número de dias do <b>primeiro</b> prazo, usado para calcular o
 * vencimento de uma conta a receber gerada automaticamente
 * ({@code vencimento = dataBase + dias}).
 *
 * <p>Cobertura:</p>
 * <ul>
 *   <li>Enum {@link PaymentCondition}: extrai o primeiro numeral do nome
 *       do enum (ex.: {@code PRAZO_30_DIAS → 30},
 *       {@code PARCELAS_30_60_90 → 30}, {@code BOLETO_15_DIAS → 15},
 *       {@code FATURADO_45_DIAS → 45}). À vista
 *       ({@code A_VISTA_DINHEIRO}, {@code PIX}, {@code BOLETO_A_VISTA})
 *       → 0.</li>
 *   <li>Texto livre (contrato manual): formatos como {@code "30"},
 *       {@code "30/60/90"}, {@code "30 dias"}, {@code "À vista"}/{@code "A vista"}.
 *       Primeiro numeral encontrado vence; à vista → 0.</li>
 *   <li>Fallback: 30 dias quando nada for parseável.</li>
 * </ul>
 */
public final class PaymentConditionParser {

    /** Fallback usado quando a condição não é parseável. */
    public static final int DEFAULT_DAYS = 30;

    private static final Pattern FIRST_NUMBER = Pattern.compile("\\d+");
    private static final int AVISTA_DAYS = 0;

    private PaymentConditionParser() {
    }

    /**
     * Resolve o primeiro prazo (em dias) a partir do enum
     * {@link PaymentCondition} de um pedido de venda ou proposta técnica.
     */
    public static int firstTermDays(PaymentCondition condition) {
        if (condition == null) {
            return DEFAULT_DAYS;
        }
        return switch (condition) {
            case A_VISTA_DINHEIRO, PIX, BOLETO_A_VISTA -> AVISTA_DAYS;
            // Enum cujo nome sempre contém o primeiro prazo como numeral.
            default -> firstNumberOrDefault(condition.name());
        };
    }

    /**
     * Resolve o primeiro prazo (em dias) a partir de um texto livre
     * (ex.: condição de pagamento de um contrato, campo opcional).
     */
    public static int firstTermDays(String condition) {
        if (condition == null || condition.isBlank()) {
            return DEFAULT_DAYS;
        }
        String trimmed = condition.trim();
        String normalized = trimmed.toLowerCase();
        if (normalized.equals("à vista") || normalized.equals("a vista")
                || normalized.equals("avista") || normalized.equals("entrada")) {
            return AVISTA_DAYS;
        }
        return firstNumberOrDefault(trimmed);
    }

    private static int firstNumberOrDefault(String text) {
        Matcher m = FIRST_NUMBER.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group());
            } catch (NumberFormatException ignored) {
                return DEFAULT_DAYS;
            }
        }
        return DEFAULT_DAYS;
    }
}