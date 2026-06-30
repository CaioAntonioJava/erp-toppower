package br.com.toppower.erp_toppower.common.util;

/**
 * Utilitário para validação de documentos fiscais brasileiros (CPF e CNPJ)
 * com verificação dos dígitos verificadores (DV).
 *
 * <p>Aceita o documento com ou sem formatação (pontos, traços, barras).
 * Retorna {@code false} para entradas nulas, vazias ou com tamanho incorreto.</p>
 *
 * <h2>Algoritmo</h2>
 * <p>Para o CPF:</p>
 * <ol>
 *   <li>Multiplica os 9 primeiros dígitos pelos pesos 10, 9, 8, 7, 6, 5, 4, 3, 2</li>
 *   <li>Soma e calcula mod 11. Se resto &lt; 2, DV = 0; senão DV = 11 - resto</li>
 *   <li>Repete para os 10 primeiros dígitos com pesos 11..2</li>
 *   <li>Compara os DVs calculados com os 2 últimos dígitos</li>
 * </ol>
 *
 * <p>Para o CNPJ: o mesmo princípio, com pesos diferentes
 * (5,4,3,2,9,8,7,6,5,4,3,2 para o 1º DV e 6,5,4,3,2,9,8,7,6,5,4,3,2 para o 2º).</p>
 */
public final class DocumentValidator {

    private DocumentValidator() {
    }

    /**
     * Valida um CPF (com ou sem formatação).
     * <p>CPF válido: 11 dígitos com dígitos verificadores corretos e
     * não é uma sequência de dígitos repetidos (ex: 111.111.111-11 é inválido).</p>
     *
     * @param cpf CPF com ou sem formatação (ex: "123.456.789-09" ou "12345678909")
     * @return {@code true} se o CPF for válido
     */
    public static boolean isValidCpf(String cpf) {
        if (cpf == null) {
            return false;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return false;
        }
        if (allSameDigit(digits)) {
            return false;
        }
        return isValidCpfDigits(digits);
    }

    /**
     * Valida um CNPJ (com ou sem formatação).
     * <p>CNPJ válido: 14 dígitos com dígitos verificadores corretos e
     * não é uma sequência de dígitos repetidos.</p>
     *
     * @param cnpj CNPJ com ou sem formatação (ex: "12.345.678/0001-90" ou "12345678000190")
     * @return {@code true} se o CNPJ for válido
     */
    public static boolean isValidCnpj(String cnpj) {
        if (cnpj == null) {
            return false;
        }
        String digits = cnpj.replaceAll("\\D", "");
        if (digits.length() != 14) {
            return false;
        }
        if (allSameDigit(digits)) {
            return false;
        }
        return isValidCnpjDigits(digits);
    }

    private static boolean allSameDigit(String digits) {
        return digits.chars().allMatch(c -> c == digits.charAt(0));
    }

    private static boolean isValidCpfDigits(String digits) {
        int firstCheck = calculateMod11CheckDigit(digits, 9, CPF_FIRST_WEIGHTS);
        if (firstCheck != Character.digit(digits.charAt(9), 10)) {
            return false;
        }
        int secondCheck = calculateMod11CheckDigit(digits, 10, CPF_SECOND_WEIGHTS);
        return secondCheck == Character.digit(digits.charAt(10), 10);
    }

    private static boolean isValidCnpjDigits(String digits) {
        int firstCheck = calculateMod11CheckDigit(digits, 12, CNPJ_FIRST_WEIGHTS);
        if (firstCheck != Character.digit(digits.charAt(12), 10)) {
            return false;
        }
        int secondCheck = calculateMod11CheckDigit(digits, 13, CNPJ_SECOND_WEIGHTS);
        return secondCheck == Character.digit(digits.charAt(13), 10);
    }

    /**
     * Calcula um dígito verificador usando o algoritmo módulo 11.
     *
     * @param digits   string contendo apenas dígitos (0-9)
     * @param length   quantos dígitos considerar (9 ou 10 para CPF; 12 ou 13 para CNPJ)
     * @param weights  array de pesos a aplicar, do mesmo tamanho de {@code length}
     * @return o dígito verificador calculado (0 a 9)
     */
    private static int calculateMod11CheckDigit(String digits, int length, int[] weights) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.digit(digits.charAt(i), 10) * weights[i];
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    private static final int[] CPF_FIRST_WEIGHTS = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CPF_SECOND_WEIGHTS = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
}
