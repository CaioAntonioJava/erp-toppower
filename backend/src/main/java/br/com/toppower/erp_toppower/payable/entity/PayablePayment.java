package br.com.toppower.erp_toppower.payable.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pagamento avulso registrado contra uma parcela de conta a pagar
 * ({@link PayableInstallment}). Cada pagamento abate do valor da
 * parcela vinculada; o service recalcula
 * {@link PayableInstallment#getPaidAmount()}, o status da parcela e,
 * em cascata, {@link Payable#getPaidAmount()} e o status da conta pai.
 *
 * <p>Não há relacionamento JPA — apenas os IDs {@link #payableId} e
 * {@link #installmentId}, em consonância com o restante do projeto.
 * Também <b>não</b> é {@code OrganizationScopedEntity}: o isolamento
 * por organização é garantido indiretamente via a conta pai.</p>
 */
@Entity
@Table(
        name = "accounts_payable_payments",
        indexes = {
                @Index(name = "idx_payable_payment_payable", columnList = "payable_id"),
                @Index(name = "idx_payable_payment_installment", columnList = "installment_id"),
                @Index(name = "idx_payable_payment_date", columnList = "payment_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PayablePayment extends BaseEntity {

    /**
     * ID da conta a pagar ({@link Payable}) a que este pagamento
     * pertence. Denormalizado da parcela para facilitar consultas.
     */
    @Column(name = "payable_id", nullable = false, updatable = false)
    private Long payableId;

    /**
     * ID da parcela ({@link PayableInstallment}) que este pagamento
     * baixa. O saldo da parcela é abatido pelo valor do pagamento.
     */
    @Column(name = "installment_id", nullable = false, updatable = false)
    private Long installmentId;

    /**
     * Valor do pagamento. Deve ser positivo.
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Data em que o pagamento foi efetivamente realizado.
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * Observações livres sobre o pagamento (ex.: forma de pagamento,
     * número do comprovante, conta bancária). Opcional.
     */
    @Column(name = "notes", length = 500)
    private String notes;
}