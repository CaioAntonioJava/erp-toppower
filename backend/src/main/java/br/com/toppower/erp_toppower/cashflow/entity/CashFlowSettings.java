package br.com.toppower.erp_toppower.cashflow.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Configuração do fluxo de caixa por organização — guarda o
 * <b>saldo inicial</b> do controle de caixa (caixa existente antes do
 * primeiro movimento registrado no sistema).
 *
 * <p>Existe um único registro por organização (unique em
 * {@code organization_id}). O saldo atual do caixa é derivado em
 * memória pelo {@code CashFlowService}:
 * {@code saldo = saldoInicial + Σ entradas − Σ saídas} sobre os
 * pagamentos efetivados de contas a receber e a pagar.</p>
 *
 * <p>O saldo inicial pode ser negativo (caixa descoberto real).</p>
 *
 * <p><b>Organization-scoped</b>: herda de {@link OrganizationScopedEntity}
 * para garantir isolamento multi-tenant via {@code organizationFilter}.</p>
 */
@Entity
@Table(
        name = "cash_flow_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cashflow_settings_org",
                columnNames = "organization_id")
)
@Getter
@Setter
@NoArgsConstructor
public class CashFlowSettings extends OrganizationScopedEntity {

    /**
     * Saldo inicial do caixa (antes do primeiro movimento registrado).
     * Default {@code 0.00} no persist. Aceita valor negativo.
     */
    @Column(name = "opening_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingBalance;

    /**
     * Inicialização padrão antes de persistir: saldo inicial ZERO quando
     * não definido pelo chamador.
     */
    @PrePersist
    private void onPrePersist() {
        if (openingBalance == null) {
            openingBalance = BigDecimal.ZERO;
        }
    }
}