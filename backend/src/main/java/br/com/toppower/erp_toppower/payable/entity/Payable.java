package br.com.toppower.erp_toppower.payable.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
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
 * Entidade que representa uma conta a pagar emitida por um fornecedor
 * (Supplier) contra a empresa (Organization).
 *
 * <p>Uma conta a pagar pode nascer de:</p>
 * <ul>
 *   <li><b>Cadastro manual</b> — sem documento de origem
 *       ({@link PayableSource#MANUAL});</li>
 *   <li><b>Boleto vinculado a um fornecedor</b> — snapshot do valor e
 *       vencimento do boleto, preservando {@link #boletoId} para
 *       rastreabilidade ({@link PayableSource#BOLETO});</li>
 *   <li><b>Nota de compra (NF-e XML)</b> — reservado para uso futuro;
 *       os campos {@link #purchaseInvoiceId} e
 *       {@link #purchaseInvoiceNumber} já existem para que a importação
 *       de XML não exija alteração de schema
 *       ({@link PayableSource#PURCHASE_INVOICE}).</li>
 * </ul>
 *
 * <p>O fornecedor é referenciado por {@link #supplierId} (obrigatório).
 * Ao contrário do contas a receber, não há exclusão mútua entre tipos
 * de devedor — o devedor é sempre um fornecedor (PJ).</p>
 *
 * <p><b>Parcelas programadas</b>: uma conta a pagar pode ter uma ou
 * mais {@link PayableInstallment}, cada uma com vencimento próprio.
 * Os pagamentos realizados são registrados em {@link PayablePayment}
 * e vinculados a uma parcela específica. O {@link #paidAmount} é a
 * soma de todos os pagamentos; o status transita para
 * {@link PayableStatus#PAGO} quando <b>todas</b> as parcelas estão
 * quitadas. O saldo devedor total é derivado em memória
 * ({@code value - paidAmount}).</p>
 *
 * <p><b>Organization-scoped</b>: herda de {@link OrganizationScopedEntity}
 * para garantir isolamento multi-tenant via {@code organizationFilter}.</p>
 */
@Entity
@Table(
        name = "accounts_payable",
        indexes = {
                @Index(name = "idx_payable_status", columnList = "status"),
                @Index(name = "idx_payable_due_date", columnList = "due_date"),
                @Index(name = "idx_payable_supplier", columnList = "supplier_id"),
                @Index(name = "idx_payable_source_type", columnList = "source_type"),
                @Index(name = "idx_payable_boleto", columnList = "boleto_id"),
                @Index(name = "idx_payable_purchase_invoice", columnList = "purchase_invoice_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Payable extends OrganizationScopedEntity {

    /**
     * Descrição/origem da conta (ex.: "Boleto fornecedor XYZ",
     * "Nota de compra NF-123"). Salvo em MAIÚSCULAS.
     */
    @UpperCase
    @Column(name = "description", nullable = false, length = 300)
    private String description;

    /**
     * Valor total da conta. Fixo após a criação — pagamentos parciais
     * abatem via {@link #paidAmount}, sem alterar este campo. Deve
     * bater com a soma dos valores das parcelas.
     */
    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    /**
     * Valor já pago (soma dos pagamentos cadastrados). Atualizado pelo
     * service a cada registro/remoção de pagamento.
     */
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    /**
     * Data de emissão da conta. Base para o cálculo dos vencimentos
     * das parcelas quando a condição de pagamento é informada em vez
     * das datas explícitas.
     */
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    /**
     * Vencimento-base da conta (snapshot do vencimento da 1ª parcela).
     * Mantido para listagens/relatórios; o vencimento real por parcela
     * está em {@link PayableInstallment#getDueDate()}.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Status atual da conta. Padrão {@link PayableStatus#ABERTO} na
     * criação. O soft delete troca para {@link PayableStatus#CANCELADO}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayableStatus status;

    /**
     * Origem da conta (manual, boleto ou nota de compra). Imutável
     * após a criação ({@code updatable = false}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false, length = 25)
    private PayableSource sourceType;

    /**
     * Referência ao {@code Supplier} (fornecedor) devedor. Obrigatório.
     */
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    /**
     * ID do boleto que originou a conta. Nulo quando
     * {@link #sourceType} != {@link PayableSource#BOLETO}. Imutável
     * após a criação.
     */
    @Column(name = "boleto_id", updatable = false)
    private Long boletoId;

    /**
     * ID da nota de compra que originou a conta. Nulo quando
     * {@link #sourceType} != {@link PayableSource#PURCHASE_INVOICE}.
     * Imutável após a criação. <b>Reservado para uso futuro</b>
     * (importação de NF-e XML).
     */
    @Column(name = "purchase_invoice_id", updatable = false)
    private Long purchaseInvoiceId;

    /**
     * Número da nota de compra que originou a conta (snapshot, ex.:
     * "NF-123"). Imutável após a criação. <b>Reservado para uso
     * futuro.</b>
     */
    @Column(name = "purchase_invoice_number", updatable = false, length = 30)
    private String purchaseInvoiceNumber;

    /**
     * Condição de pagamento acordada com o fornecedor. Reutiliza o enum
     * do módulo de propostas comerciais. Opcional.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_condition", length = 50)
    private PaymentCondition paymentCondition;

    /**
     * Quantidade de parcelas programadas. Default 1 (à vista) na
     * criação manual sem parcelas explícitas.
     */
    @Column(name = "installments_count", nullable = false)
    private int installmentsCount;

    /**
     * Data do último pagamento registrado (ou data de quitação).
     * Nula enquanto a conta não recebeu pagamentos.
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * Inicialização padrão antes de persistir: status ABERTO,
     * paidAmount ZERO e installmentsCount 1 quando não definidos.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = PayableStatus.ABERTO;
        }
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        if (installmentsCount <= 0) {
            installmentsCount = 1;
        }
    }
}