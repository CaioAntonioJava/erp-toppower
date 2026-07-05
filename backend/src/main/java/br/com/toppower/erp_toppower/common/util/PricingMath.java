package br.com.toppower.erp_toppower.common.util;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Matemática de precificação compartilhada entre documentos comerciais
 * (propostas, pedidos de venda e futuras ordens de serviço).
 *
 * <p>Reúne funções puras (sem estado, sem dependências de persistência)
 * que aplicam margem de lucro e desconto global sobre um subtotal de
 * itens. As entidades de domínio ({@code Quotation}, {@code SalesOrder},
 * etc.) mantêm seus próprios campos ({@code profitMargin},
 * {@code discount}, {@code discountType}, {@code freightValue}) e
 * delegam aqui apenas o cálculo, evitando duplicação de fórmulas.</p>
 *
 * <h2>Regra de composição do total</h2>
 * <p>Para documentos que expõem a margem (como {@code Quotation} e a
 * futura ordem de serviço), a ordem de aplicação é:</p>
 * <ol>
 *   <li>margem: {@code subtotal × (1 + profitMargin / 100)} — incide só
 *       sobre o total dos itens;</li>
 *   <li>desconto global: aplicado sobre o valor já com margem, como
 *       valor fixo (R$) ou percentual (%);</li>
 *   <li>frete: somado ao final, não participa da margem nem do
 *       desconto.</li>
 * </ol>
 * <p>Pedidos de venda que <b>embutem</b> a margem nos preços dos itens
 * (em vez de guardá-la como campo) usam apenas
 * {@link #profitFactor(BigDecimal)} na conversão e
 * {@link #applyGlobalDiscount(BigDecimal, BigDecimal, DiscountType)} no
 * recálculo — sem chamar {@link #applyProfitMargin}.</p>
 */
public final class PricingMath {

    private PricingMath() {
    }

    /**
     * Aplica a margem de lucro como multiplicação percentual sobre a
     * base, através do fator {@code (1 + profitMargin / 100)}.
     *
     * <p>Retorna a própria base (arredondada para 2 casas) quando a margem
     * for nula ou zero, e {@code ZERO} quando a base for nula.</p>
     *
     * @param base         valor sobre o qual a margem incide (ex.: subtotal
     *                     dos itens)
     * @param profitMargin margem em porcentagem (ex.: {@code 10.00} = 10%);
     *                     nula ou zero significa "sem margem"
     * @return base majorada pelo fator de margem, em 2 casas decimais
     */
    public static BigDecimal applyProfitMargin(BigDecimal base, BigDecimal profitMargin) {
        if (base == null) {
            return BigDecimal.ZERO;
        }
        if (profitMargin == null || profitMargin.signum() == 0) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = profitFactor(profitMargin);
        return base.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Aplica o desconto global sobre a base, interpretando o valor
     * conforme {@code discountType}: valor monetário fixo
     * ({@link DiscountType#AMOUNT}) ou percentual
     * ({@link DiscountType#PERCENT}).
     *
     * <p>Retorna a própria base (arredondada para 2 casas) quando o
     * desconto ou o tipo forem nulos, e {@code ZERO} quando a base for
     * nula.</p>
     *
     * @param base         valor já com margem (ou o subtotal, quando o
     *                     documento não usa margem)
     * @param discount     valor do desconto (R$ ou % conforme o tipo)
     * @param discountType interpretação do desconto; nulo desativa o desconto
     * @return base com o desconto aplicado, em 2 casas decimais
     */
    public static BigDecimal applyGlobalDiscount(BigDecimal base, BigDecimal discount, DiscountType discountType) {
        if (base == null) {
            return BigDecimal.ZERO;
        }
        if (discount == null || discountType == null || discount.signum() == 0) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }
        return switch (discountType) {
            case AMOUNT -> base.subtract(discount).setScale(2, RoundingMode.HALF_UP);
            case PERCENT -> base.subtract(
                            base.multiply(discount)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);
        };
    }

    /**
     * Fator multiplicativo {@code (1 + profitMargin / 100)} usado para
     * majorar preços pela margem de lucro. Retorna {@code 1} quando a
     * margem for nula ou zero.
     *
     * <p>Útil em conversões de documento que embutem a margem nos preços
     * dos itens (ex.: proposta → pedido de venda), preservando a
     * identidade {@code unitPrice * quantity - discount = totalPrice}.</p>
     *
     * @param profitMargin margem em porcentagem; nula ou zero retorna 1
     * @return fator majorador com 4 casas de precisão intermediária
     */
    public static BigDecimal profitFactor(BigDecimal profitMargin) {
        if (profitMargin == null || profitMargin.signum() == 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE.add(
                profitMargin.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }
}