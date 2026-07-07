package br.com.toppower.erp_toppower.sales.salesorder.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.common.util.PricingMath;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.salesorder.enums.SalesOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa um pedido de venda emitido pela empresa para
 * um cliente pessoa física ({@code Customer}) ou jurídica ({@code Company}).
 *
 * <p>Pedidos podem nascer de duas formas:</p>
 * <ul>
 *   <li><b>Conversão de proposta</b> — snapshot dos dados de uma
 *       {@code Quotation} ATIVA, preservando {@link #quotationUuid} e
 *       {@link #quotationNumber} para rastreabilidade. Após a conversão
 *       o pedido evolui independentemente da proposta.</li>
 *   <li><b>Criação direta</b> — sem proposta de origem
 *       ({@link #quotationUuid} e {@link #quotationNumber} nulos).</li>
 * </ul>
 *
 * <p>O número do pedido é gerado pelo serviço de aplicação no momento
 * do cadastro, iniciando em {@code 1000} e incrementando em +1 a cada
 * novo pedido. É um valor numérico ({@code Long}), não pode ser
 * alterado via JPA ({@code updatable = false}) e deve ser único no
 * sistema.</p>
 *
 * <p>O comprador é referenciado por exatamente <b>um</b> dos campos
 * {@link #customerUuid} ou {@link #companyUuid}; o serviço de aplicação
 * é responsável por validar essa invariante antes de persistir.</p>
 *
 * <p>Os itens do pedido são mantidos em uma entidade separada
 * ({@code SalesOrderItem}) e carregados em memória pelo serviço — este
 * agregado <b>não</b> declara relacionamento JPA com os itens, em
 * consonância com o restante do projeto.</p>
 *
 * <p><b>Importante:</b> diferentemente da proposta, o pedido <b>não</b>
 * possui margem de lucro ({@code profitMargin}). O pedido é o documento
 * externo enviado ao cliente (PDF), e a margem de lucro é informação
 * interna de precificação mantida apenas na {@code Quotation}.</p>
 *
 * <p>Os campos calculados {@link #subtotal}, {@link #total} e
 * {@link #totalQuantity} são preenchidos em memória pelo serviço após
 * carregar os itens, através de {@link #recalculateTotals(List)}. O
 * desconto por item <b>já está</b> subtraído em
 * {@code item.totalPrice}, de modo que o {@code subtotal} reflete o
 * total líquido dos itens. O desconto global é então subtraído do
 * subtotal, e por último o {@link #freightValue} é somado — o frete
 * nunca participa do desconto.</p>
 */
@Entity
@Table(
        name = "sales_orders",
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(
                        name = "uk_sales_order_number",
                        columnNames = "number")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SalesOrder extends OrganizationScopedEntity {

    /**
     * Número sequencial do pedido, sem prefixo. Ex.: {@code 1000},
     * {@code 1001}, {@code 1002}, ...
     *
     * <p>Gerado automaticamente pelo serviço a partir do maior número
     * já emitido, iniciando em {@code 1000} no primeiro pedido.
     * Imutável via JPA ({@code updatable = false}) e único no sistema.</p>
     */
    @Column(name = "number", nullable = false, updatable = false)
    private Long number;

    /**
     * Data de emissão do pedido (data comercial, não timestamp).
     *
     * <p>Preenchida automaticamente em {@link #onPrePersist()} com a data
     * atual do calendário no momento da persistência. Imutável após a
     * criação ({@code updatable = false}).</p>
     */
    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDate orderDate;

    /**
     * Referência ao {@code Customer} (pessoa física) comprador do pedido.
     *
     * <p>Deve ser preenchido <b>apenas</b> quando o comprador for pessoa
     * física, em conjunto com {@link #companyUuid} nulo. Validação da
     * invariante "exatamente um preenchido" é responsabilidade do
     * serviço de aplicação.</p>
     */
    @Column(name = "customer_uuid")
    private UUID customerUuid;

    /**
     * Referência à {@code Company} (pessoa jurídica) compradora do pedido.
     *
     * <p>Deve ser preenchido <b>apenas</b> quando o comprador for pessoa
     * jurídica, em conjunto com {@link #customerUuid} nulo.</p>
     */
    @Column(name = "company_uuid")
    private UUID companyUuid;

    /**
     * "Aos cuidados de:" — nome da pessoa de contato no lado do
     * comprador a quem o pedido deve ser direcionado. Opcional.
     *
     * <p>Salvo sempre em MAIÚSCULAS pelo
     * {@code UpperCaseFieldListener} (registrado em
     * {@link BaseEntity}), independente de como foi enviado pelo
     * cliente.</p>
     */
    @UpperCase
    @Column(name = "attention", length = 150)
    private String attention;

    /**
     * Referência ao {@code Seller} (vendedor) responsável pelo pedido.
     * Obrigatório.
     */
    @Column(name = "seller_uuid", nullable = false)
    private UUID sellerUuid;

    /**
     * Tipo de aplicação do desconto global ({@link #discount}).
     * Nulo quando não há desconto.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    /**
     * Valor do desconto global aplicado sobre o subtotal dos itens.
     *
     * <p>O valor é interpretado de acordo com {@link #discountType}:
     * como valor monetário fixo (R$) ou percentual (%). Nulo quando não
     * há desconto.</p>
     */
    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount;

    /**
     * Condição de pagamento acordada com o comprador. Opcional.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_condition", length = 50)
    private PaymentCondition paymentCondition;

    /**
     * Observações livres do pedido (ex.: instruções de entrega,
     * garantias acordadas, etc.). Opcional.
     */
    @Column(name = "notes", length = 2000)
    private String notes;

    /**
     * Tipo de frete (CIF/FOB). Opcional.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "freight_type", length = 10)
    private FreightType freightType;

    /**
     * Valor do frete informado manualmente. É somado ao total do pedido
     * após o desconto global — nunca participa do desconto. Opcional.
     */
    @Column(name = "freight_value", precision = 10, scale = 2)
    private BigDecimal freightValue;

    /**
     * Situação atual do pedido no seu ciclo de vida. Padrão
     * {@link SalesOrderStatus#ABERTO} na criação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SalesOrderStatus status;

    /**
     * Referência à {@code Carrier} (transportadora) responsável pelo
     * frete do pedido. Opcional — documentos sem transportadora
     * permanecem com este campo nulo. Não há FK física (referência por
     * UUID, padrão do projeto); a validação de existência é feita no
     * service quando o campo é informado.
     */
    @Column(name = "carrier_uuid")
    private UUID carrierUuid;

    /**
     * UUID da {@code Quotation} que deu origem a este pedido. Nulo em
     * pedidos criados diretamente (sem proposta de origem). Imutável
     * após a criação ({@code updatable = false}).
     */
    @Column(name = "quotation_uuid", updatable = false)
    private UUID quotationUuid;

    /**
     * Número da {@code Quotation} que deu origem a este pedido (snapshot
     * do número no momento da conversão). Nulo em pedidos criados
     * diretamente. Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "quotation_number", updatable = false)
    private Long quotationNumber;

    // ---------------------------------------------------------------------
    // Campos calculados (não persistidos)
    // ---------------------------------------------------------------------

    /**
     * Soma dos totais líquidos de cada item (já considerando o desconto
     * por item), antes do desconto global. Preenchido em memória por
     * {@link #recalculateTotals(List)}.
     */
    @Transient
    private BigDecimal subtotal = BigDecimal.ZERO;

    /**
     * Total final do pedido. Preenchido em memória por
     * {@link #recalculateTotals(List)}. Regra de composição:
     * {@code (subtotal − desconto global) + frete}. O desconto global é
     * aplicado sobre o subtotal dos itens; o frete é somado por último e
     * nunca participa do desconto.
     *
     * <p>Diferente da proposta, <b>sem</b> margem de lucro — o total do
     * pedido reflete o valor cobrado do cliente.</p>
     */
    @Transient
    private BigDecimal total = BigDecimal.ZERO;

    /**
     * Soma das quantidades de todos os itens do pedido (unidades
     * comercializadas, não linhas). Preenchido em memória por
     * {@link #recalculateTotals(List)}.
     */
    @Transient
    private Integer totalQuantity = 0;

    /**
     * Recalcula os campos {@link #subtotal}, {@link #total} e
     * {@link #totalQuantity} a partir da lista de itens.
     *
     * <p>Deve ser invocado pelo serviço após carregar os itens, antes
     * de mapear a entidade para o DTO de resposta.</p>
     *
     * <p>Regra de cálculo:</p>
     * <ul>
     *   <li>{@code subtotal} = soma de {@code item.totalPrice} de cada
     *       item (já líquido do desconto por item, que é deduzido na
     *       criação/atualização do item);</li>
     *   <li>{@code total} = {@code subtotal} menos o desconto global
     *       (valor ou percentual), e por último somado ao
     *       {@code freightValue};</li>
     *   <li>{@code totalQuantity} = soma de {@code item.quantity}.</li>
     * </ul>
     *
     * @param items itens do pedido (pode ser nulo ou vazia)
     */
    public void recalculateTotals(List<SalesOrderItem> items) {
        if (items == null || items.isEmpty()) {
            this.subtotal = BigDecimal.ZERO;
            this.totalQuantity = 0;
        } else {
            this.subtotal = items.stream()
                    .map(SalesOrderItem::getTotalPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            this.totalQuantity = items.stream()
                    .map(SalesOrderItem::getQuantity)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .intValue();
        }
        // total = subtotal − desconto global, e por último somado o frete.
        // O frete é repassado de forma fixa, sem participar do desconto.
        // Sem margem de lucro — o total reflete o valor cobrado do cliente.
        BigDecimal discounted = applyGlobalDiscount(this.subtotal);
        BigDecimal freight = (freightValue != null) ? freightValue : BigDecimal.ZERO;
        this.total = discounted.add(freight).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Valor em R$ do desconto global efetivamente aplicado, derivado da
     * mesma base usada em {@link #recalculateTotals(List)}: diferença entre
     * o subtotal e o subtotal já descontado. Retorna {@code BigDecimal.ZERO}
     * quando não há desconto global configurado.
     *
     * <p>Exposto para que o mapper possa popular o DTO de resposta sem
     * duplicar a lógica de cálculo.</p>
     */
    public BigDecimal calculateGlobalDiscountValue() {
        BigDecimal discounted = applyGlobalDiscount(this.subtotal);
        return this.subtotal.subtract(discounted).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyGlobalDiscount(BigDecimal base) {
        return PricingMath.applyGlobalDiscount(base, this.discount, this.discountType);
    }

    /**
     * Inicialização padrão antes de persistir: preenche a data de emissão
     * com a data atual e o status com {@link SalesOrderStatus#ABERTO}
     * caso ainda não tenham sido definidos pelo chamador. Não
     * sobrescreve valores previamente atribuídos.
     */
    @PrePersist
    private void onPrePersist() {
        if (orderDate == null) {
            orderDate = LocalDate.now();
        }
        if (status == null) {
            status = SalesOrderStatus.ABERTO;
        }
    }
}