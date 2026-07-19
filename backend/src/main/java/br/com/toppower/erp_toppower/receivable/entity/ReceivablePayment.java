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
 * Pagamento avulso registrado contra uma conta a receber
 * ({@link Receivable}). Cada pagamento abate do valor total da conta
 * pai; o service recalcula {@code Receivable.paidAmount} e o status
 * (transita para {@code PAGO} quando o saldo devedor zera).
 *
 * <p>Não há relacionamento JPA com a conta pai — apenas o ID
 * ({@link #receivableId}), em consonância com o restante do projeto
 * (itens de pedido/proposta seguem o mesmo padrão).</p>
 *
 * <p><b>Não</b> é {@code OrganizationScopedEntity}: o isolamento por
 * organização é garantido indiretamente via a conta pai (carregada
 * sempre escopada pelo {@code organizationFilter}).</p>
 */
@Entity
@Table(
        name = "accounts_receivable_payments",
        indexes = {
                @Index(name = "idx_receivable_payment_receivable", columnList = "receivable_id"),
                @Index(name = "idx_receivable_payment_date", columnList = "payment_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReceivablePayment extends BaseEntity {

    /**
     * ID da conta a receber ({@link Receivable}) a que este pagamento
     * pertence. Não há FK física (convenção do projeto).
     */
    @Column(name = "receivable_id", nullable = false, updatable = false)
    private Long receivableId;

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