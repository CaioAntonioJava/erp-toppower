package br.com.toppower.erp_toppower.sales.quotation.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.common.util.PricingMath;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus;
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
 * Entidade que representa uma proposta comercial (orçamento) emitida pela
 * empresa para um cliente pessoa física ({@code Customer}) ou jurídica
 * ({@code Company}).
 *
 * <p>O número da proposta é gerado pelo serviço de aplicação no momento
 * do cadastro, iniciando em {@code 1500} e incrementando em +1 a cada
 * nova proposta. É um valor numérico ({@code Long}), não pode ser
 * alterado via JPA ({@code updatable = false}) e deve ser único no
 * sistema.</p>
 *
 * <p>O comprador é referenciado por exatamente <b>um</b> dos campos
 * {@link #customerUuid} ou {@link #companyUuid}; o serviço de aplicação
 * é responsável por validar essa invariante antes de persistir.</p>
 *
 * <p>Os itens da proposta são mantidos em uma entidade separada
 * ({@code QuotationItem}) e carregados em memória pelo serviço — este
 * agregado <b>não</b> declara relacionamento JPA com os itens, em
 * consonância com o restante do projeto.</p>
 *
* <p>Os campos calculados {@link #subtotal}, {@link #total} e
 *     {@link #totalQuantity} são preenchidos em memória pelo serviço
 *     após carregar os itens, através de {@link #recalculateTotals(List)}.
 *     O desconto por item <b>já está</b> subtraído em
 *     {@code item.totalPrice}, e a margem de lucro <b>já está</b>
 *     embutida no {@code item.unitPrice} e refletida no
 *     {@code item.totalPrice} — ambos calculados pelo
 *     {@code QuotationMapper} no momento da criação/atualização do item.
 *     Com isso, o {@code subtotal} já reflete o total líquido dos itens
 *     com margem, o desconto global é então subtraído, e por último o
 *     {@link #freightValue} é somado — o frete nunca participa da margem
 *     nem do desconto.</p>
 */
@Entity
@Table(
        name = "quotations",
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(
                        name = "uk_quotation_org_number",
                        columnNames = {"organization_uuid", "number"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Quotation extends OrganizationScopedEntity {

    /**
     * Número sequencial da proposta, sem prefixo. Ex.: {@code 1500},
     * {@code 1501}, {@code 1502}, ...
     *
     * <p>Gerado automaticamente pelo serviço a partir do maior número
     * já emitido, iniciando em {@code 1500} na primeira proposta.
     * Imutável via JPA ({@code updatable = false}) e único no sistema.
     * Caso seja necessário corrigir o número manualmente (operação
     * administrativa), isso deve ser feito por endpoint dedicado que
     * realize um UPDATE nativo contornando a restrição de coluna.</p>
     */
    @Column(name = "number", nullable = false, updatable = false)
    private Long number;

    /**
     * Data de emissão da proposta (data comercial, não timestamp).
     *
     * <p>Preenchida automaticamente em {@link #onPrePersist()} com a data
     * atual do calendário no momento da persistência. Imutável após a
     * criação ({@code updatable = false}).</p>
     */
    @Column(name = "issue_date", nullable = false, updatable = false)
    private LocalDate issueDate;

    /**
     * Referência ao {@code Customer} (pessoa física) comprador da proposta.
     *
     * <p>Deve ser preenchido <b>apenas</b> quando o comprador for pessoa
     * física, em conjunto com {@link #companyUuid} nulo. Validação da
     * invariante "exatamente um preenchido" é responsabilidade do
     * serviço de aplicação.</p>
     */
    @Column(name = "customer_uuid")
    private UUID customerUuid;

    /**
     * Referência à {@code Company} (pessoa jurídica) compradora da proposta.
     *
     * <p>Deve ser preenchido <b>apenas</b> quando o comprador for pessoa
     * jurídica, em conjunto com {@link #customerUuid} nulo.</p>
     */
    @Column(name = "company_uuid")
    private UUID companyUuid;

    /**
     * "Aos cuidados de:" — nome da pessoa de contato no lado do
     * comprador a quem a proposta deve ser direcionada. Opcional.
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
     * Referência ao {@code Seller} (vendedor) responsável pela proposta.
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
     * Prazo de validade da proposta, em dias, contado a partir de
     * {@link #issueDate}. Nulo significa "sem prazo definido".
     */
    @Column(name = "validity_days")
    private Integer validityDays;

    /**
     * Condição de pagamento acordada com o comprador. Opcional.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_condition", length = 50)
    private PaymentCondition paymentCondition;

    /**
     * Observações livres da proposta (ex.: instruções de entrega,
     * garantias acordadas, etc.). Opcional.
     */
    @Column(name = "notes", length = 2000)
    private String notes;

    /**
     * Situação atual da proposta no seu ciclo de vida. Padrão
     * {@link QuotationStatus#ATIVA} na criação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuotationStatus status;

    /**
     * Tipo de frete (CIF/FOB). Opcional.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "freight_type", length = 10)
    private FreightType freightType;

    /**
     * Valor do frete informado manualmente. É somado ao total da proposta
     * após o desconto global — nunca participa do desconto. Opcional.
     */
    @Column(name = "freight_value", precision = 10, scale = 2)
    private BigDecimal freightValue;

    /**
     * Referência à {@code Carrier} (transportadora) responsável pelo
     * frete da proposta. Opcional — documentos sem frete/transportadora
     * permanecem com este campo nulo. Não há FK física (referência por
     * UUID, padrão do projeto); a validação de existência é feita no
     * service quando o campo é informado.
     */
    @Column(name = "carrier_uuid")
    private UUID carrierUuid;

    /**
     * Margem de lucro aplicada na proposta, expressa em porcentagem
     * (ex.: {@code 10.00} = 10%). Obrigatória.
     *
     * <p>É aplicada <b>item a item</b> pelo {@code QuotationMapper} no
     * momento da criação/atualização dos itens, majorando o
     * {@code unitPrice} de cada {@link QuotationItem} pelo fator
     * {@code (1 + profitMargin / 100)} e refletindo o resultado no
     * {@code totalPrice} do item. Com isso, o {@link #total} da proposta
     * passa a ser simplesmente
     * {@code subtotal − desconto global + frete}, sendo {@code subtotal}
     * a soma dos totais líquidos dos itens (já com margem embutida).</p>
     *
     * <p>Nota: registros criados antes da refatoração podem ter
     * {@code unitPrice}/{@code totalPrice} sem margem — esses itens não
     * serão retroativamente recalculados.</p>
     */
    @Column(name = "profit_margin", nullable = false, precision = 5, scale = 2)
    private BigDecimal profitMargin;

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
     * Total final da proposta. Preenchido em memória por
     * {@link #recalculateTotals(List)}. Regra de composição:
     * {@code subtotal − desconto global + frete}, sendo {@code subtotal}
     * a soma dos {@code totalPrice} dos itens (já com margem de lucro
     * embutida e já líquido do desconto por item). O desconto global é
     * aplicado sobre o valor já com margem; o frete é somado por último
     * e nunca participa da margem nem do desconto.
     */
    @Transient
    private BigDecimal total = BigDecimal.ZERO;

    /**
     * Soma das quantidades de todos os itens da proposta (unidades
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
     *       item. Cada {@code totalPrice} já reflete (a) a margem de
     *       lucro embutida no {@code unitPrice} e (b) o desconto da
     *       própria linha já subtraído — ambos aplicados pelo
     *       {@code QuotationMapper} na criação/atualização do item;</li>
     *   <li>{@code total} = {@code subtotal} menos o desconto global
     *       (valor fixo ou percentual, aplicado sobre o valor já com
     *       margem) e somado ao {@link #freightValue}. O frete nunca
     *       participa da margem nem do desconto.</li>
     *   <li>{@code totalQuantity} = soma de {@code item.quantity}.</li>
     * </ul>
     *
     * @param items itens da proposta (pode ser nulo ou vazia)
     */
    public void recalculateTotals(List<QuotationItem> items) {
        if (items == null || items.isEmpty()) {
            this.subtotal = BigDecimal.ZERO;
            this.totalQuantity = 0;
        } else {
            this.subtotal = items.stream()
                    .map(QuotationItem::getTotalPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            this.totalQuantity = items.stream()
                    .map(QuotationItem::getQuantity)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .intValue();
        }
        // total = subtotal (já com margem embutida nos itens) − desconto global
        // + frete. O desconto global é aplicado sobre o valor já com margem;
        // o frete é somado ao final, sem participar da margem nem do desconto.
        BigDecimal discounted = PricingMath.applyGlobalDiscount(
                this.subtotal, this.discount, this.discountType);
        BigDecimal freight = (freightValue != null) ? freightValue : BigDecimal.ZERO;
        this.total = discounted.add(freight).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Valor em R$ do desconto global efetivamente aplicado, derivado da
     * mesma base usada em {@link #recalculateTotals(List)}: diferença entre
     * o subtotal (já com margem) e o subtotal já descontado. Retorna
     * {@code BigDecimal.ZERO} quando não há desconto global configurado.
     *
     * <p>Exposto para que o mapper possa popular o DTO de resposta sem
     * duplicar a lógica de cálculo.</p>
     */
    public BigDecimal calculateGlobalDiscountValue() {
        BigDecimal discounted = PricingMath.applyGlobalDiscount(
                this.subtotal, this.discount, this.discountType);
        return this.subtotal.subtract(discounted).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Inicialização padrão antes de persistir: preenche a data de emissão
     * com a data atual e o status com {@link QuotationStatus#ATIVA} caso
     * ainda não tenham sido definidos pelo chamador. Não sobrescreve
     * valores previamente atribuídos.
     */
    @PrePersist
    private void onPrePersist() {
        if (issueDate == null) {
            issueDate = LocalDate.now();
        }
        if (status == null) {
            status = QuotationStatus.ATIVA;
        }
    }
}
