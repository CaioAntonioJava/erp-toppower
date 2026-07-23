package br.com.toppower.erp_toppower.purchase.util;

import java.text.Normalizer;

/**
 * Utilitários de similaridade entre textos, usados no matching de
 * produtos na importação de NF-e. Implementação própria, sem
 * dependências externas.
 */
public final class SimilarityUtil {

    private SimilarityUtil() {
    }

    /**
     * Normaliza um texto para comparação: lowercase, sem acentos,
     * sem pontuação e com espaços colapsados.
     */
    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // Separa acentos e remove os não-ASCII (diacríticos).
        String s = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Mantém apenas letras, números e espaços.
        s = s.replaceAll("[^a-zA-Z0-9 ]", " ");
        // Colapsa espaços múltiplos.
        s = s.replaceAll("\\s+", " ").toLowerCase().trim();
        return s;
    }

    /**
     * Razão de similaridade entre duas strings baseada na distância de
     * Levenshtein. Retorna um valor entre 0.0 (totalmente diferente) e
     * 1.0 (idênticas). Compara os textos normalizados.
     */
    public static double similarity(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.isEmpty() && nb.isEmpty()) {
            return 1.0;
        }
        if (na.isEmpty() || nb.isEmpty()) {
            return 0.0;
        }
        int maxLen = Math.max(na.length(), nb.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int distance = levenshtein(na, nb);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Distância de Levenshtein (custo unitário por inserção/remoção/
     * substituição). Implementação iterativa com dois arrays.
     */
    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}