package br.com.toppower.erp_toppower.common.util;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testes unitários do {@link PricingMath}.
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>Desconto percentual e em valor fixo</li>
 *   <li>Base nula retorna ZERO</li>
 *   <li>Desconto nulo ou zero preserva a base</li>
 *   <li>Arredondamento para 2 casas decimais</li>
 * </ul>
 */
class PricingMathTest {

    @Test
    void applyGlobalDiscount_baseNula_retornaZero() {
        BigDecimal result = PricingMath.applyGlobalDiscount(null, BigDecimal.TEN, DiscountType.AMOUNT);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void applyGlobalDiscount_descontoNulo_preservaBase() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("100.00"), null, DiscountType.AMOUNT);
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void applyGlobalDiscount_descontoZero_preservaBase() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("100.00"), BigDecimal.ZERO, DiscountType.PERCENT);
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void applyGlobalDiscount_discountTypeNulo_preservaBase() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("100.00"), BigDecimal.TEN, null);
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void applyGlobalDiscount_descontoPercentual_aplicaCorretamente() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("200.00"), new BigDecimal("10"), DiscountType.PERCENT);
        // 200 - 10% = 200 - 20 = 180
        assertEquals(new BigDecimal("180.00"), result);
    }

    @Test
    void applyGlobalDiscount_descontoValorFixo_aplicaCorretamente() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("200.00"), new BigDecimal("30.00"), DiscountType.AMOUNT);
        assertEquals(new BigDecimal("170.00"), result);
    }

    @Test
    void applyGlobalDiscount_descontoPercentualArredondamento_duasCasas() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("100.00"), new BigDecimal("33.33"), DiscountType.PERCENT);
        // 100 - 33.33% = 100 - 33.33 = 66.67
        assertEquals(new BigDecimal("66.67"), result);
    }

    @Test
    void applyGlobalDiscount_baseComCasas_preservaPrecisao() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("99.99"), new BigDecimal("10"), DiscountType.PERCENT);
        // 99.99 - 10% = 99.99 - 9.999 = 89.991 -> 89.99
        assertEquals(new BigDecimal("89.99"), result);
    }

    @Test
    void applyGlobalDiscount_descontoMaiorQueBase_retornaNegativo() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("50.00"), new BigDecimal("100.00"), DiscountType.AMOUNT);
        assertEquals(new BigDecimal("-50.00"), result);
    }

    @Test
    void applyGlobalDiscount_semDesconto_retornaBaseComDuasCasas() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("100.999"), null, null);
        assertEquals(new BigDecimal("101.00"), result);
    }

    @Test
    void applyGlobalDiscount_baseComEscalaDiferente_normalizaParaDuasCasas() {
        BigDecimal result = PricingMath.applyGlobalDiscount(new BigDecimal("150.1234"), BigDecimal.ZERO, DiscountType.PERCENT);
        assertEquals(new BigDecimal("150.12"), result);
    }
}
