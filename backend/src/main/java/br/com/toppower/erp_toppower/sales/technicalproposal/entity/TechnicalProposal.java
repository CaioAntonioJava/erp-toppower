package br.com.toppower.erp_toppower.sales.technicalproposal.entity;

import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.common.entity.TenantScopedEntity;
import br.com.toppower.erp_toppower.common.util.PricingMath;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa uma proposta técnica emitida pela empresa para
 * um cliente pessoa física ({@code Customer}) ou jurídica ({@code Company}).
 *
 * <p>O identificador comercial da proposta é composto por três campos
 * persistidos — {@link #prefix} (ex.: {@code "PL"}), {@link #sequence}
 * (numérico sequencial) e {@link #year} (ano corrente) — e exibido no
 * formato {@code PL-001-2026} através de {@link #formattedCode()}. A
 * sequência reseta a cada novo ano (volta para {@code 1} quando o ano
 * muda). A trinca {@code (prefix, sequence, year)} é única no sistema.</p>
 *
 * <p>O cliente é referenciado por exatamente <b>um</b> dos campos
 * {@link #customerUuid} ou {@link #companyUuid}; o serviço de aplicação
 * valida essa invariante antes de persistir.</p>
 *
 * <p>Os itens da proposta são mantidos em entidades separadas
 * ({@link TechnicalProposalServiceItem} e {@link TechnicalProposalProductItem})
 * e carregados em memória pelo serviço — este agregado <b>não</b> declara
 * relacionamento JPA com os itens, em consonância com o restante do
 * projeto (ver {@code Quotation}).</p>
 *
 * <p>Os campos calculados {@link #subtotal}, {@link #total},
 * {@link #servicesSubtotal}, {@link #productsSubtotal} e
 * {@link #globalDiscountValue} são preenchidos em memória pelo serviço
 * após carregar os itens, através de
 * {@link #recalculateTotals(List, List)}.</p>
 *
 * <h2>Regra de composição do total</h2>
 * <ol>
 *   <li>subtotal = soma dos preços dos serviços + soma dos totais líquidos
 *       dos produtos;</li>
 *   <li>margem: {@code subtotal × (1 + profitMargin / 100)} — incide só
 *       sobre o subtotal dos itens;</li>
 *   <li>desconto global: aplicado sobre o valor já com margem, como valor
 *       fixo (R$) ou percentual (%);</li>
 *   <li>frete: somado ao final, não participa da margem nem do desconto.</li>
 * </ol>
 */
@Entity
@Table(
        name = "technical_proposals",
        indexes = {
                @Index(name = "idx_technical_proposal_status", columnList = "status"),
                @Index(name = "idx_technical_proposal_start_date", columnList = "start_date"),
                @Index(name = "idx_technical_proposal_customer", columnList = "customer_uuid"),
                @Index(name = "idx_technical_proposal_company", columnList = "company_uuid")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_technical_proposal_code",
                        columnNames = {"prefix", "sequence", "year"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TechnicalProposal extends TenantScopedEntity {

    /** Prefixo fixo do código de proposta técnica. Hoje sempre {@code "PL"}. */
    public static final String DEFAULT_PREFIX = "PL";

    /**
     * Prefixo do código comercial (ex.: {@code "PL"}). Imutável após a
     * criação ({@code updatable = false}).
     */
    @Column(name = "prefix", nullable = false, updatable = false, length = 10)
    private String prefix;

    /**
     * Numeral sequencial do código, reiniciando em {@code 1} a cada novo
     * ano. Exibido com 3 dígitos no código formatado ({@code PL-001-2026}).
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "sequence", nullable = false, updatable = false)
    private Long sequence;

    /**
     * Ano de emissão da proposta, parte final do código
     * (ex.: {@code 2026} em {@code PL-001-2026}). Imutável após a criação
     * ({@code updatable = false}).
     */
    @Column(name = "year", nullable = false, updatable = false)
    private Integer year;

    /**
     * Referência ao {@code Customer} (pessoa física) comprador da proposta.
     * Deve ser preenchido <b>apenas</b> quando o comprador for pessoa
     * física, em conjunto com {@link #companyUuid} nulo.
     */
    @Column(name = "customer_uuid")
    private UUID customerUuid;

    /**
     * Referência à {@code Company} (pessoa jurídica) compradora da proposta.
     * Deve ser preenchido <b>apenas</b> quando o comprador for pessoa
     * jurídica, em conjunto com {@link #customerUuid} nulo.
     */
    @Column(name = "company_uuid")
    private UUID companyUuid;

    /**
     * Endereço onde o serviço será executado. <b>Opcional</b> — todos os
     * campos do {@link Address} são nullable aqui, ao contrário do
     * {@code Customer}/{@code Company} onde são obrigatórios.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street",
                    column = @Column(name = "address_street", length = 200)),
            @AttributeOverride(name = "number",
                    column = @Column(name = "address_number", length = 20)),
            @AttributeOverride(name = "complement",
                    column = @Column(name = "address_complement", length = 100)),
            @AttributeOverride(name = "neighborhood",
                    column = @Column(name = "address_neighborhood", length = 100)),
            @AttributeOverride(name = "city",
                    column = @Column(name = "address_city", length = 100)),
            @AttributeOverride(name = "state",
                    column = @Column(name = "address_state", length = 2)),
            @AttributeOverride(name = "zipCode",
                    column = @Column(name = "address_zip_code", length = 9))
    })
    private Address address;

    /**
     * Objetivos do serviço prestado — descrições curtas, de uma linha,
     * do que se propõe a executar. Mantidos como uma lista persistida em
     * {@link TechnicalProposalObjective} separada desta entidade, em
     * consonância com o restante do projeto (serviços/produtos). O serviço
     * carrega a lista e a expõe no DTO de resposta.
     */
    // (sem campo persistido aqui — relacionamento por UUID na entidade filha)

    /**
     * Descrição detalhada do serviço prestado, usada para formalização.
     * Texto livre mais longo que os objetivos.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Status atual da proposta no seu ciclo de vida. Padrão
     * {@link TechnicalProposalStatus#ABERTA} na criação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TechnicalProposalStatus status;

    /**
     * Data de início da proposta (data comercial, não timestamp).
     * Obrigatória; recebe {@code LocalDate.now()} no {@link #onPrePersist()}
     * caso não tenha sido informada.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Data de término prevista/real do serviço, informada manualmente pelo
     * usuário no formulário. <b>Opcional</b> — distinta da
     * {@link #deliveryDate}, que é preenchida automaticamente quando a
     * proposta passa ao status {@link TechnicalProposalStatus#CONCLUIDA}.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Data de entrega, preenchida automaticamente quando a proposta passa
     * ao status {@link TechnicalProposalStatus#CONCLUIDA} (via endpoint
     * dedicado). Nula enquanto a proposta não for concluída.
     */
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    /**
     * Margem de lucro aplicada sobre o subtotal dos itens, expressa em
     * porcentagem (ex.: {@code 10.00} = 10%). Obrigatória. Incide apenas
     * sobre o subtotal dos itens, antes do desconto global e do frete.
     */
    @Column(name = "profit_margin", nullable = false, precision = 5, scale = 2)
    private BigDecimal profitMargin;

    /**
     * Tipo de aplicação do desconto global ({@link #discount}). Nulo
     * quando não há desconto.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    /**
     * Valor do desconto global, interpretado conforme {@link #discountType}
     * (valor fixo em R$ ou percentual). Nulo quando não há desconto.
     */
    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount;

    /**
     * Valor do frete informado manualmente. É somado ao total após a
     * margem e o desconto global — nunca participa de nenhum dos dois.
     */
    @Column(name = "freight_value", precision = 10, scale = 2)
    private BigDecimal freightValue;

    /**
     * Prazo de entrega em texto livre (ex.: {@code "3 dias"},
     * {@code "2 semanas"}). Não é numérico para permitir flexibilidade na
     * forma de expressar o prazo.
     */
    @Column(name = "delivery_deadline", length = 50)
    private String deliveryDeadline;

    /**
     * Condição de pagamento acordada com o comprador. Opcional. Reutiliza
     * o enum do módulo de propostas comerciais.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_condition", length = 50)
    private PaymentCondition paymentCondition;

    /**
     * Validade da proposta em texto livre (ex.: {@code "10 dias"},
     * {@code "15 dias"}).
     */
    @Column(name = "validity", length = 50)
    private String validity;

    /**
     * Tipo de entrega (CIF/FOB), mesmo domínio usado em cotação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", length = 10)
    private FreightType deliveryType;

    /**
     * Observações livres da proposta (ex.: instruções de execução,
     * garantias acordadas, etc.). Opcional.
     */
    @Column(name = "notes", length = 2000)
    private String notes;

    // ---------------------------------------------------------------------
    // Campos calculados (não persistidos)
    // ---------------------------------------------------------------------

    /** Soma dos preços dos serviços prestados. */
    @Transient
    private BigDecimal servicesSubtotal = BigDecimal.ZERO;

    /** Soma dos totais líquidos dos produtos. */
    @Transient
    private BigDecimal productsSubtotal = BigDecimal.ZERO;

    /** Subtotal geral = servicesSubtotal + productsSubtotal. */
    @Transient
    private BigDecimal subtotal = BigDecimal.ZERO;

    /**
     * Total final da proposta: {@code (subtotal × (1 + margem/100))
     * − desconto global + frete}.
     */
    @Transient
    private BigDecimal total = BigDecimal.ZERO;

    /**
     * Valor em R$ do desconto global efetivamente aplicado, derivado da
     * diferença entre o subtotal com margem e o subtotal já descontado.
     */
    @Transient
    private BigDecimal globalDiscountValue = BigDecimal.ZERO;

    // ---------------------------------------------------------------------
    // Código formatado
    // ---------------------------------------------------------------------

    /**
     * Código de exibição no formato {@code PL-001-2026}, derivado de
     * {@link #prefix}, {@link #sequence} e {@link #year}.
     */
    public String formattedCode() {
        return String.format("%s-%03d-%d", prefix, sequence, year);
    }

    // ---------------------------------------------------------------------
    // Recálculo dos totais
    // ---------------------------------------------------------------------

    /**
     * Recalcula os campos {@link #servicesSubtotal}, {@link #productsSubtotal},
     * {@link #subtotal}, {@link #total} e {@link #globalDiscountValue} a
     * partir das listas de serviços e produtos. Deve ser invocado pelo
     * serviço após carregar os itens, antes de mapear a entidade para o
     * DTO de resposta.
     *
     * @param serviceItems itens de serviço (pode ser nulo ou vazia)
     * @param productItems  itens de produto (pode ser nulo ou vazia)
     */
    public void recalculateTotals(List<TechnicalProposalServiceItem> serviceItems,
                                  List<TechnicalProposalProductItem> productItems) {
        this.servicesSubtotal = sumServices(serviceItems);
        this.productsSubtotal = sumProducts(productItems);
        this.subtotal = this.servicesSubtotal.add(this.productsSubtotal)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal withMargin = PricingMath.applyProfitMargin(this.subtotal, this.profitMargin);
        BigDecimal discounted = PricingMath.applyGlobalDiscount(withMargin, this.discount, this.discountType);
        BigDecimal freight = (freightValue != null) ? freightValue : BigDecimal.ZERO;
        this.globalDiscountValue = withMargin.subtract(discounted).setScale(2, RoundingMode.HALF_UP);
        this.total = discounted.add(freight).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal sumServices(List<TechnicalProposalServiceItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(TechnicalProposalServiceItem::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal sumProducts(List<TechnicalProposalProductItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(TechnicalProposalProductItem::getTotalPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------------
    // PrePersist
    // ---------------------------------------------------------------------

    /**
     * Inicialização padrão antes de persistir: define o prefixo como
     * {@link #DEFAULT_PREFIX}, o status como {@link TechnicalProposalStatus#ABERTA}
     * e a data de início como {@code LocalDate.now()} quando não tenham
     * sido definidos pelo chamador. Não sobrescreve valores previamente
     * atribuídos.
     */
    @PrePersist
    private void onPrePersist() {
        if (prefix == null) {
            prefix = DEFAULT_PREFIX;
        }
        if (status == null) {
            status = TechnicalProposalStatus.ABERTA;
        }
        if (startDate == null) {
            startDate = LocalDate.now();
        }
    }
}