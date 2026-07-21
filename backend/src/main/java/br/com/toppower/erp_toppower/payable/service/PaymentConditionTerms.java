package br.com.toppower.erp_toppower.payable.service;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário estático que interpreta uma condição de pagamento e devolve
 * a lista completa de prazos (em dias) de cada parcela. Usado para gerar
 * as parcelas programadas de uma conta a pagar quando o usuário informa
 * a condição de pagamento mas não as parcelas explícitas.
 *
 * <p>Cobertura:</p>
 * <ul>
 *   <li>Enum {@link PaymentCondition}: extrai todos os numerais do nome
 *       do enum (ex.: {@code PARCELAS_30_60_90 → [30, 60, 90]},
 *       {@code PRAZO_30_DIAS → [30]}, {@code A_VISTA_DINHEIRO → [0]}).</li>
 *   <li>Texto livre: {@code "30/60/90"} → {@code [30, 60, 90]};
 *       {@code "À vista"} → {@code [0]}; {@code "30 dias"} → {@code [30]}.</li>
 *   <li>Fallback: {@code [DEFAULT_DAYS]} (30 dias) quando nada for
 *       parseável.</li>
 * </ul>
 *
 * <p>Para condições com entrada ({@code ENTRADA_MAIS_30_60_DIAS}), a
 * entrada não é tratada como parcela programada aqui — apenas os
 * prazos após a entrada são retornados. O service decide como lidar
 * com a entrada (tipicamente uma parcela com vencimento 0 = hoje).</p>
 */
public final class PaymentConditionTerms {

    /** Fallback usado quando a condição não é parseável. */
    public static final int DEFAULT_DAYS = 30;

    private static final Pattern NUMBERS = Pattern.compile("\\d+");
    private static final int AVISTA_DAYS = 0;

    private PaymentConditionTerms() {
    }

    /**
     * Resolve a lista de prazos (em dias) de cada parcela a partir do
     * enum {@link PaymentCondition}. À vista devolve {@code [0]}.
     */
    public static List<Integer> terms(PaymentCondition condition) {
        if (condition == null) {
            return List.of(DEFAULT_DAYS);
        }
        return switch (condition) {
            case A_VISTA_DINHEIRO, PIX, BOLETO_A_VISTA -> List.of(AVISTA_DAYS);
            // Enum cujo nome sempre contém os prazos como numerais
            // (ex.: PARCELAS_30_60_90, BOLETO_15_DIAS, FATURADO_45_DIAS).
            default -> numbersOf(condition.name());
        };
    }

    /**
     * Resolve a lista de prazos (em dias) a partir de um texto livre
     * (ex.: condição de pagamento de um contrato, campo opcional).
     */
    public static List<Integer> terms(String condition) {
        if (condition == null || condition.isBlank()) {
            return List.of(DEFAULT_DAYS);
        }
        String trimmed = condition.trim();
        String normalized = trimmed.toLowerCase();
        if (normalized.equals("à vista") || normalized.equals("a vista")
                || normalized.equals("avista")) {
            return List.of(AVISTA_DAYS);
        }
        List<Integer> nums = numbersOf(trimmed);
        return nums.isEmpty() ? List.of(DEFAULT_DAYS) : nums;
    }

    private static List<Integer> numbersOf(String text) {
        Matcher m = NUMBERS.matcher(text);
        List<Integer> result = new ArrayList<>();
        while (m.find()) {
            try {
                result.add(Integer.parseInt(m.group()));
            } catch (NumberFormatException ignored) {
                // ignora numerais inválidos
            }
        }
        return result;
    }
}