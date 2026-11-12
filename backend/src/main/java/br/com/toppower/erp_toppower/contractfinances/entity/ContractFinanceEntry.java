package br.com.toppower.erp_toppower.contractfinances.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryStatus;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
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
 * Lançamento do financeiro de um contrato: um gasto ou um recebimento
 * ({@link #type}) com data livre, valor positivo e descrição opcional,
 * vinculado a um controle de contrato
 * ({@link ContractFinanceControl}, pendurado no
 * {@code Contract}).
 *
 * <p>O vínculo é feito por {@link #controlId}, sem FK física (convenção
 * do projeto). O soft delete apenas marca {@link #status} = INATIVO;
 * lançamentos inativos permanecem no histórico, mas não somam nos
 * totais do controle.</p>
 *
 * <p><b>Organization-scoped</b>: herda de {@link OrganizationScopedEntity}
 * para o isolamento multi-tenant via {@code organizationFilter}.</p>
 */
@Entity
@Table(
        name = "contract_finance_entries",
        indexes = {
                @Index(name = "idx_cfentry_control", columnList = "control_id"),
                @Index(name = "idx_cfentry_date", columnList = "entry_date"),
                @Index(name = "idx_cfentry_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ContractFinanceEntry extends OrganizationScopedEntity {

    /**
     * ID do {@link ContractFinanceControl} dono do lançamento. Sem FK
     * física (convenção do projeto). Imutável: o lançamento pertence
     * sempre ao mesmo contrato.
     */
    @Column(name = "control_id", nullable = false, updatable = false)
    private Long controlId;

    /**
     * Natureza do lançamento: {@code RECEBIDO} = entrada; demais
     * categorias = gasto.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ContractFinanceEntryType type;

    /** Data em que a movimentação (gasto/recebimento) aconteceu. */
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    /** Valor do lançamento. Sempre positivo (o sentido é dado pelo tipo). */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Descrição livre do gasto/recebimento (ex.: "cabo 50mm", "pedágio rodovia"). */
    @Column(name = "description", length = 500)
    private String description;

    /** Status do lançamento. Padrão ATIVO no backfill/persist. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractFinanceEntryStatus status;

    /** Inicialização padrão: status = ATIVO quando não definido. */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = ContractFinanceEntryStatus.ATIVO;
        }
    }
}
