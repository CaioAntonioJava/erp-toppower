package br.com.toppower.erp_toppower.common.util;

import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Matemática de precificação compartilhada entre documentos comerciais
 * (propostas, pedidos de venda e futuras ordens de serviço).
 *
 * <p>Reúne funções puras (sem estado, sem dependências de persistência)
 * que aplicam o desconto global sobre uma base. As entidades de domínio
 * ({@code Quotation}, {@code SalesOrder}, etc.) mantêm seus próprios
 * campos ({@code discount}, {@code discountType}, {@code freightValue}) e
 * delegam aqui apenas o cálculo, evitando duplicação de fórmulas.</p>
 *
 * <h2>Arquitetura da margem de lucro</h2>
 * <p>A margem de lucro ({@code profitMargin}) <b>não é</b> mais aplicada
 * aqui — ela é responsabilidade dos mappers de item
 * ({@code QuotationMapper}, {@code TechnicalProposalMapper}), que
 * majoram o {@code unitPrice}/{@code price} de cada linha no momento da
 * criação/atualização do documento e refletem o resultado no
 * {@code totalPrice} persistido. Com isso, o total do documento passa a
 * ser simplesmente:</p>
 * <ol>
 *   <li>{@code subtotal} = soma dos {@code totalPrice} dos itens (já com
 *       margem embutida e já líquido do desconto por item);</li>
 *   <li>desconto global: aplicado sobre o {@code subtotal} (já com
 *       margem), como valor fixo (R$) ou percentual (%);</li>
 *   <li>frete: somado ao final, não participa da margem nem do
 *       desconto.</li>
 * </ol>
 * <p>Pedidos de venda originados de cotação apenas copiam os preços
 * finais dos itens — a margem já está embutida no snapshot persistido
 * na cotação.</p>
 */
public final class PricingMath {

    private PricingMath() {
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
     * @param base         valor já com margem embutida nos itens (ou
     *                     apenas o subtotal, no caso de documentos que
     *                     não usam margem)
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
}