package br.com.toppower.erp_toppower.payable.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
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
 * Parcela programada de uma conta a pagar ({@link Payable}). Cada
 * parcela tem vencimento e valor próprios. Os pagamentos realizados
 * ({@link PayablePayment}) são vinculados a uma parcela específica;
 * o service recalcula {@link #paidAmount} e o status da parcela
 * (transita para {@link PayableStatus#PAGO} quando o saldo zera).
 *
 * <p>Uma conta a pagar com parcelamento 30/60/90 terá três parcelas,
 * cada uma com seu vencimento. O status da conta pai transita para
 * {@link PayableStatus#PAGO} apenas quando <b>todas</b> as parcelas
 * estão quitadas.</p>
 *
 * <p>Não há relacionamento JPA com a conta pai — apenas o ID
 * ({@link #payableId}), em consonância com o restante do projeto.
 * Também <b>não</b> é {@code OrganizationScopedEntity}: o isolamento
 * por organização é garantido indiretamente via a conta pai
 * (carregada sempre escopada pelo {@code organizationFilter}).</p>
 */
@Entity
@Table(
        name = "accounts_payable_installments",
        indexes = {
                @Index(name = "idx_payable_installment_payable", columnList = "payable_id"),
                @Index(name = "idx_payable_installment_due_date", columnList = "due_date"),
                @Index(name = "idx_payable_installment_status", columnList = "status")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payable_installment",
                columnNames = {"payable_id", "installment_number"})
)
@Getter
@Setter
@NoArgsConstructor
public class PayableInstallment extends BaseEntity {

    /**
     * ID da conta a pagar ({@link Payable}) a que esta parcela
     * pertence. Não há FK física (convenção do projeto).
     */
    @Column(name = "payable_id", nullable = false, updatable = false)
    private Long payableId;

    /**
     * Número sequencial da parcela (1..N). Único por conta a pagar.
     */
    @Column(name = "installment_number", nullable = false, updatable = false)
    private int installmentNumber;

    /**
     * Valor da parcela. Deve ser positivo. A soma de todas as parcelas
     * deve bater com {@link Payable#getValue()}.
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Vencimento programado da parcela.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Valor já pago desta parcela (soma dos {@link PayablePayment}
     * vinculados). Atualizado pelo service.
     */
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    /**
     * Status da parcela. Padrão {@link PayableStatus#ABERTO}; transita
     * para {@link PayableStatus#PAGO} quando quitada. Pode ser
     * {@link PayableStatus#CANCELADO} individualmente.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayableStatus status;

    /**
     * Data do último pagamento registrado para esta parcela (ou null
     * se ainda não houve pagamentos).
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;
}