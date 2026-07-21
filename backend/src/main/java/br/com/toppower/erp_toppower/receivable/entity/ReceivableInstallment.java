package br.com.toppower.erp_toppower.receivable.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parcela programada de uma conta a receber ({@link Receivable}). Cada
 * parcela tem vencimento e valor próprios. Os pagamentos realizados
 * ({@link ReceivablePayment}) são vinculados a uma parcela específica;
 * o service recalcula {@link #paidAmount} e o status da parcela
 * (transita para {@link ReceivableStatus#PAGO} quando o saldo zera).
 *
 * <p>Uma conta a receber com parcelamento 30/60/90 terá três parcelas,
 * cada uma com seu vencimento. O status da conta pai transita para
 * {@link ReceivableStatus#PAGO} apenas quando <b>todas</b> as parcelas
 * estão quitadas.</p>
 *
 * <p>Não há relacionamento JPA com a conta pai — apenas o ID
 * ({@link #receivableId}), em consonância com o restante do projeto.
 * Também <b>não</b> é {@code OrganizationScopedEntity}: o isolamento
 * por organização é garantido indiretamente via a conta pai
 * (carregada sempre escopada pelo {@code organizationFilter}).</p>
 */
@Entity
@Table(
        name = "accounts_receivable_installments",
        indexes = {
                @Index(name = "idx_receivable_installment_receivable", columnList = "receivable_id"),
                @Index(name = "idx_receivable_installment_due_date", columnList = "due_date"),
                @Index(name = "idx_receivable_installment_status", columnList = "status")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_receivable_installment",
                columnNames = {"receivable_id", "installment_number"})
)
@Getter
@Setter
@NoArgsConstructor
public class ReceivableInstallment extends BaseEntity {

    /**
     * ID da conta a receber ({@link Receivable}) a que esta parcela
     * pertence. Não há FK física (convenção do projeto).
     */
    @Column(name = "receivable_id", nullable = false, updatable = false)
    private Long receivableId;

    /**
     * Número sequencial da parcela (1..N). Único por conta a receber.
     */
    @Column(name = "installment_number", nullable = false, updatable = false)
    private int installmentNumber;

    /**
     * Valor da parcela. Deve ser positivo. A soma de todas as parcelas
     * deve bater com {@link Receivable#getValue()}.
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Vencimento programado da parcela.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Valor já recebido desta parcela (soma dos {@link ReceivablePayment}
     * vinculados). Atualizado pelo service.
     */
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    /**
     * Status da parcela. Padrão {@link ReceivableStatus#ABERTO}; transita
     * para {@link ReceivableStatus#PAGO} quando quitada. Pode ser
     * {@link ReceivableStatus#CANCELADO} individualmente.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReceivableStatus status;

    /**
     * Data do último pagamento registrado para esta parcela (ou null
     * se ainda não houve pagamentos).
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;
}