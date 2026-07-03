package br.com.toppower.erp_toppower.sales.quotation.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Condição de pagamento aplicável à proposta comercial.
 *
 * <p>Agrupada em cinco famílias para facilitar a leitura da API:</p>
 * <ul>
 *   <li><b>À vista:</b> {@link #A_VISTA_DINHEIRO}, {@link #PIX}, {@link #BOLETO_A_VISTA}.</li>
 *   <li><b>Boleto parcelado (uma parcela):</b> {@link #BOLETO_15_DIAS} a {@link #BOLETO_90_DIAS}.</li>
 *   <li><b>Prazo único (uma parcela, fora do boleto):</b> {@link #PRAZO_7_DIAS} a {@link #PRAZO_90_DIAS}.</li>
 *   <li><b>Entrada + parcelas:</b> {@link #ENTRADA_MAIS_30_DIAS},
 *       {@link #ENTRADA_MAIS_30_60_DIAS}, {@link #ENTRADA_MAIS_30_60_90_DIAS}.</li>
 *   <li><b>Parcelamento múltiplo:</b> {@link #PARCELAS_15_30_45}, {@link #PARCELAS_28_56_84},
 *       {@link #PARCELAS_30_45_60}, {@link #PARCELAS_30_60}, {@link #PARCELAS_30_60_90},
 *       {@link #PARCELAS_30_60_90_120}, {@link #PARCELAS_30_60_90_120_150}.</li>
 *   <li><b>Faturado:</b> {@link #FATURADO_30_DIAS}, {@link #FATURADO_45_DIAS},
 *       {@link #FATURADO_60_DIAS}, {@link #FATURADO_90_DIAS}.</li>
 * </ul>
 */
@Schema(name = "PaymentCondition", description = "Condição de pagamento da proposta.",
        allowableValues = {
                // À vista
                "A_VISTA_DINHEIRO", "PIX", "BOLETO_A_VISTA",
                // Boleto parcelado
                "BOLETO_15_DIAS", "BOLETO_28_DIAS", "BOLETO_30_DIAS",
                "BOLETO_45_DIAS", "BOLETO_60_DIAS", "BOLETO_90_DIAS",
                // Prazo único
                "PRAZO_7_DIAS", "PRAZO_14_DIAS", "PRAZO_15_DIAS", "PRAZO_21_DIAS",
                "PRAZO_28_DIAS", "PRAZO_30_DIAS", "PRAZO_45_DIAS", "PRAZO_60_DIAS", "PRAZO_90_DIAS",
                // Entrada + parcelas
                "ENTRADA_MAIS_30_DIAS", "ENTRADA_MAIS_30_60_DIAS", "ENTRADA_MAIS_30_60_90_DIAS",
                // Parcelamento múltiplo
                "PARCELAS_30_60", "PARCELAS_30_60_90", "PARCELAS_30_60_90_120",
                "PARCELAS_15_30_45", "PARCELAS_28_56_84", "PARCELAS_30_45_60",
                "PARCELAS_30_60_90_120_150",
                // Faturado
                "FATURADO_30_DIAS", "FATURADO_45_DIAS", "FATURADO_60_DIAS", "FATURADO_90_DIAS"
        })
public enum PaymentCondition {

    // ---------------------------------------------------------------------
    // À vista
    // ---------------------------------------------------------------------
    /** Pagamento à vista em dinheiro. */
    A_VISTA_DINHEIRO("À Vista (Dinheiro)"),
    /** Pagamento à vista via PIX. */
    PIX("PIX"),
    /** Boleto bancário com vencimento à vista. */
    BOLETO_A_VISTA("Boleto à Vista"),

    // ---------------------------------------------------------------------
    // Boleto — uma parcela (N dias)
    // ---------------------------------------------------------------------
    /** Boleto com vencimento para 15 dias. */
    BOLETO_15_DIAS("Boleto 15 Dias"),
    /** Boleto com vencimento para 28 dias. */
    BOLETO_28_DIAS("Boleto 28 Dias"),
    /** Boleto com vencimento para 30 dias. */
    BOLETO_30_DIAS("Boleto 30 Dias"),
    /** Boleto com vencimento para 45 dias. */
    BOLETO_45_DIAS("Boleto 45 Dias"),
    /** Boleto com vencimento para 60 dias. */
    BOLETO_60_DIAS("Boleto 60 Dias"),
    /** Boleto com vencimento para 90 dias. */
    BOLETO_90_DIAS("Boleto 90 Dias"),

    // ---------------------------------------------------------------------
    // Prazo único (N dias)
    // ---------------------------------------------------------------------
    /** Pagamento único em 7 dias. */
    PRAZO_7_DIAS("7 Dias"),
    /** Pagamento único em 14 dias. */
    PRAZO_14_DIAS("14 Dias"),
    /** Pagamento único em 15 dias. */
    PRAZO_15_DIAS("15 Dias"),
    /** Pagamento único em 21 dias. */
    PRAZO_21_DIAS("21 Dias"),
    /** Pagamento único em 28 dias. */
    PRAZO_28_DIAS("28 Dias"),
    /** Pagamento único em 30 dias. */
    PRAZO_30_DIAS("30 Dias"),
    /** Pagamento único em 45 dias. */
    PRAZO_45_DIAS("45 Dias"),
    /** Pagamento único em 60 dias. */
    PRAZO_60_DIAS("60 Dias"),
    /** Pagamento único em 90 dias. */
    PRAZO_90_DIAS("90 Dias"),

    // ---------------------------------------------------------------------
    // Entrada + parcelas
    // ---------------------------------------------------------------------
    /** Entrada seguida de uma parcela em 30 dias. */
    ENTRADA_MAIS_30_DIAS("Entrada + 30 Dias"),
    /** Entrada seguida de parcelas em 30 e 60 dias. */
    ENTRADA_MAIS_30_60_DIAS("Entrada + 30 + 60 Dias"),
    /** Entrada seguida de parcelas em 30, 60 e 90 dias. */
    ENTRADA_MAIS_30_60_90_DIAS("Entrada + 30 + 60 + 90 Dias"),

    // ---------------------------------------------------------------------
    // Parcelamento múltiplo (sem entrada)
    // ---------------------------------------------------------------------
    /** Parcelas em 30 e 60 dias. */
    PARCELAS_30_60("30/60 Dias"),
    /** Parcelas em 30, 60 e 90 dias. */
    PARCELAS_30_60_90("30/60/90 Dias"),
    /** Parcelas em 30, 60, 90 e 120 dias. */
    PARCELAS_30_60_90_120("30/60/90/120 Dias"),
    /** Parcelas em 15, 30 e 45 dias. */
    PARCELAS_15_30_45("15/30/45 Dias"),
    /** Parcelas em 28, 56 e 84 dias. */
    PARCELAS_28_56_84("28/56/84 Dias"),
    /** Parcelas em 30, 45 e 60 dias. */
    PARCELAS_30_45_60("30/45/60 Dias"),
    /** Parcelas em 30, 60, 90, 120 e 150 dias. */
    PARCELAS_30_60_90_120_150("30/60/90/120/150 Dias"),

    // ---------------------------------------------------------------------
    // Faturado
    // ---------------------------------------------------------------------
    /** Faturado com vencimento para 30 dias. */
    FATURADO_30_DIAS("Faturado para 30 Dias"),
    /** Faturado com vencimento para 45 dias. */
    FATURADO_45_DIAS("Faturado para 45 Dias"),
    /** Faturado com vencimento para 60 dias. */
    FATURADO_60_DIAS("Faturado para 60 Dias"),
    /** Faturado com vencimento para 90 dias. */
    FATURADO_90_DIAS("Faturado para 90 Dias");

    private final String displayName;

    PaymentCondition(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Nome de exibição em português, conforme rótulo exibido no documento
     * da proposta (ex.: {@code "30/60 Dias"}).
     */
    public String getDisplayName() {
        return displayName;
    }
}
