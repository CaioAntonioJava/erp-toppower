package br.com.toppower.erp_toppower.receivable.entity;

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
 * Pagamento avulso registrado contra uma parcela de conta a receber
 * ({@link ReceivableInstallment}). Cada pagamento abate do valor da
 * parcela vinculada; o service recalcula
 * {@link ReceivableInstallment#getPaidAmount()}, o status da parcela e,
 * em cascata, {@link Receivable#getPaidAmount()} e o status da conta pai.
 *
 * <p>Para contas a receber criadas antes do modelo de parcelas (ou para
 * a parcela única à vista), {@link #installmentId} pode ser nulo — nesse
 * caso o pagamento abate diretamente do saldo da conta pai.</p>
 *
 * <p>Não há relacionamento JPA — apenas os IDs {@link #receivableId} e
 * {@link #installmentId}, em consonância com o restante do projeto.
 * Também <b>não</b> é {@code OrganizationScopedEntity}: o isolamento por
 * organização é garantido indiretamente via a conta pai (carregada
 * sempre escopada pelo {@code organizationFilter}).</p>
 */
@Entity
@Table(
        name = "accounts_receivable_payments",
        indexes = {
                @Index(name = "idx_receivable_payment_receivable", columnList = "receivable_id"),
                @Index(name = "idx_receivable_payment_installment", columnList = "installment_id"),
                @Index(name = "idx_receivable_payment_date", columnList = "payment_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReceivablePayment extends BaseEntity {

    /**
     * ID da conta a receber ({@link Receivable}) a que este pagamento
     * pertence. Denormalizado da parcela para facilitar consultas.
     * Não há FK física (convenção do projeto).
     */
    @Column(name = "receivable_id", nullable = false, updatable = false)
    private Long receivableId;

    /**
     * ID da parcela ({@link ReceivableInstallment}) que este pagamento
     * baixa. O saldo da parcela é abatido pelo valor do pagamento.
     * Nulo para pagamentos de contas antigas sem parcela vinculada
     * (mantidos para retrocompatibilidade).
     */
    @Column(name = "installment_id", updatable = false)
    private Long installmentId;

    /**
     * Valor do pagamento. Deve ser positivo.
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Data em que o pagamento foi efetivamente recebido.
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * Observações livres sobre o pagamento (ex.: forma de recebimento,
     * número do comprovante). Opcional.
     */
    @Column(name = "notes", length = 500)
    private String notes;
}