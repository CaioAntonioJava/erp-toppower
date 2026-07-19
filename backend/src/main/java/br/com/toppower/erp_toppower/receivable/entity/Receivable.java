package br.com.toppower.erp_toppower.receivable.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade que representa uma conta a receber emitida pela empresa
 * (Organization) para um cliente pessoa física (Customer) ou jurídica
 * (Company).
 *
 * <p>Uma conta a receber pode nascer de três formas automáticas ou de
 * cadastro manual:</p>
 * <ul>
 *   <li><b>Conversão de proposta em pedido de venda</b> — snapshot do
 *       valor do pedido, preservando {@link #salesOrderId} e
 *       {@link #salesOrderNumber} para rastreabilidade
 *       ({@link ReceivableSource#SALES_ORDER});</li>
 *   <li><b>Conclusão de proposta técnica</b> — snapshot do valor da
 *       proposta, preservando {@link #technicalProposalId} e
 *       {@link #technicalProposalCode}
 *       ({@link ReceivableSource#TECHNICAL_PROPOSAL});</li>
 *   <li><b>Conclusão de contrato</b> — snapshot do preço do contrato,
 *       preservando {@link #contractId} e {@link #contractCode}
 *       ({@link ReceivableSource#CONTRACT});</li>
 *   <li><b>Cadastro manual</b> — sem documento de origem
 *       ({@link ReceivableSource#MANUAL}).</li>
 * </ul>
 *
 * <p>O cliente é referenciado por exatamente <b>um</b> dos campos
 * {@link #customerId} ou {@link #companyId}; o serviço de aplicação
 * valida essa invariante antes de persistir (contas geradas
 * automaticamente herdam o cliente do documento de origem).</p>
 *
 * <p><b>Pagamentos parciais</b>: o valor total ({@link #value}) é fixo;
 * pagamentos avulsos são cadastrados em {@link ReceivablePayment} e
 * somados em {@link #paidAmount} pelo service. O status transita para
 * {@link ReceivableStatus#PAGO} automaticamente quando
 * {@code paidAmount >= value}. O saldo devedor é derivado em memória
 * ({@code value - paidAmount}).</p>
 *
 * <p><b>Organization-scoped</b>: herda de {@link OrganizationScopedEntity}
 * para garantir isolamento multi-tenant via {@code organizationFilter}.</p>
 */
@Entity
@Table(
        name = "accounts_receivable",
        indexes = {
                @Index(name = "idx_receivable_status", columnList = "status"),
                @Index(name = "idx_receivable_due_date", columnList = "due_date"),
                @Index(name = "idx_receivable_customer", columnList = "customer_id"),
                @Index(name = "idx_receivable_company", columnList = "company_id"),
                @Index(name = "idx_receivable_source_type", columnList = "source_type"),
                @Index(name = "idx_receivable_sales_order", columnList = "sales_order_id"),
                @Index(name = "idx_receivable_technical_proposal", columnList = "technical_proposal_id"),
                @Index(name = "idx_receivable_contract", columnList = "contract_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Receivable extends OrganizationScopedEntity {

    /**
     * Descrição/origem da conta (ex.: "Contrato CL-001-2026",
     * "Pedido de Venda 1003"). Salvo em MAIÚSCULAS.
     */
    @UpperCase
    @Column(name = "description", nullable = false, length = 300)
    private String description;

    /**
     * Valor total da conta. Fixo após a criação — pagamentos parciais
     * abatem via {@link #paidAmount}, sem alterar este campo.
     */
    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    /**
     * Valor já recebido (soma dos pagamentos cadastrados). Atualizado
     * pelo service a cada registro/remoção de pagamento.
     */
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    /**
     * Data de vencimento da conta.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Status atual da conta. Padrão {@link ReceivableStatus#ABERTO} na
     * criação. O soft delete e a reabertura do documento de origem trocam
     * para {@link ReceivableStatus#CANCELADO}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReceivableStatus status;

    /**
     * Origem da conta (manual, pedido de venda, proposta técnica ou
     * contrato). Imutável após a criação ({@code updatable = false}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false, length = 25)
    private ReceivableSource sourceType;

    /**
     * Referência ao {@code Customer} (pessoa física) devedor. Mutuamente
     * exclusivo com {@link #companyId}.
     */
    @Column(name = "customer_id")
    private Long customerId;

    /**
     * Referência à {@code Company} (pessoa jurídica) devedora. Mutuamente
     * exclusivo com {@link #customerId}.
     */
    @Column(name = "company_id")
    private Long companyId;

    /**
     * Condição de pagamento (texto livre informativo, ex.: "30/60/90").
     * Opcional.
     */
    @UpperCase
    @Column(name = "payment_condition", length = 100)
    private String paymentCondition;

    /**
     * ID do pedido de venda que originou a conta. Nulo quando
     * {@link #sourceType} != {@link ReceivableSource#SALES_ORDER}.
     * Imutável após a criação.
     */
    @Column(name = "sales_order_id", updatable = false)
    private Long salesOrderId;

    /**
     * Número do pedido de venda que originou a conta (snapshot).
     * Imutável após a criação.
     */
    @Column(name = "sales_order_number", updatable = false)
    private Long salesOrderNumber;

    /**
     * ID da proposta técnica que originou a conta. Nulo quando
     * {@link #sourceType} != {@link ReceivableSource#TECHNICAL_PROPOSAL}.
     * Imutável após a criação.
     */
    @Column(name = "technical_proposal_id", updatable = false)
    private Long technicalProposalId;

    /**
     * Código formatado da proposta técnica que originou a conta
     * (snapshot, ex.: "PL-001-2026"). Imutável após a criação.
     */
    @Column(name = "technical_proposal_code", updatable = false, length = 30)
    private String technicalProposalCode;

    /**
     * ID do contrato que originou a conta. Nulo quando
     * {@link #sourceType} != {@link ReceivableSource#CONTRACT}.
     * Imutável após a criação.
     */
    @Column(name = "contract_id", updatable = false)
    private Long contractId;

    /**
     * Código formatado do contrato que originou a conta (snapshot,
     * ex.: "CL-001-2026"). Imutável após a criação.
     */
    @Column(name = "contract_code", updatable = false, length = 30)
    private String contractCode;

    /**
     * Data do último pagamento registrado (ou data de quitação).
     * Nula enquanto a conta não recebeu pagamentos.
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * Inicialização padrão antes de persistir: status ABERTO e
     * paidAmount ZERO quando não definidos pelo chamador.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = ReceivableStatus.ABERTO;
        }
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
    }
}