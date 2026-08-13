package br.com.toppower.erp_toppower.boleto.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade que representa um boleto cadastrado no sistema.
 *
 * <p>Herda {@link OrganizationScopedEntity} (Long + auditoria + isolamento
 * multi-tenant via {@code organization_id}). Os campos de negócio são:</p>
 * <ul>
 *   <li>{@code contractWorkNumber} — Nº da obra/contrato vinculado ao boleto.</li>
 *   <li>{@code responsibleName} — nome do responsável pelo boleto.</li>
 *   <li>{@code supplierId} — empresa (fornecedor) vinculada ao boleto.</li>
 *   <li>{@code invoiceNumber} — número da nota fiscal vinculada.</li>
 *   <li>{@code invoiceDate} — data da nota fiscal.</li>
 *   <li>{@code installmentNumber} — número da parcela (manual ou automático).</li>
 *   <li>{@code value} — valor da parcela do boleto.</li>
 *   <li>{@code dueDate} — data de vencimento da parcela.</li>
 *   <li>{@code paymentDate} — data do pagamento (quando liquidado).</li>
 *   <li>{@code status} — ATIVO/INATIVO (soft delete).</li>
 * </ul>
 */
@Entity
@Table(name = "boletos")
@Getter
@Setter
@NoArgsConstructor
public class Boleto extends OrganizationScopedEntity {

    /**
     * Nº da obra/contrato vinculado ao boleto (campo livre, opcional).
     * Permite associar o boleto a um contrato ou obra específica para
     * relatórios e conciliação. Salvo em MAIÚSCULAS.
     */
    @UpperCase
    @Column(name = "contract_work_number", length = 60)
    private String contractWorkNumber;

    /**
     * Nome do responsável pelo boleto (campo livre, opcional).
     * Salvo em MAIÚSCULAS (UpperCaseFieldListener) quando presente.
     */
    @UpperCase
    @Column(name = "responsible_name", length = 120)
    private String responsibleName;

    /**
     * Valor da parcela do boleto. Obrigatório.
     */
    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    /**
     * Data de vencimento da parcela. Obrigatória.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status;

    /**
     * Referência opcional ao {@code Supplier} (empresa/fornecedor) vinculado
     * ao boleto. Quando presente, o cadastro/edição do boleto dispara
     * a geração automática de uma conta a pagar no módulo payable.
     * Nulo quando o boleto é standalone (sem vínculo com fornecedor).
     */
    @Column(name = "supplier_id")
    private Long supplierId;

    /**
     * Indica se o boleto foi liquidado (pago). Default {@code false}.
     */
    @Column(name = "paid", nullable = false)
    private boolean paid;

    /**
     * Data em que o boleto foi liquidado (pago). Nula enquanto não pago.
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * Número da nota fiscal vinculada ao boleto (campo livre, opcional).
     * Salvo em MAIÚSCULAS (UpperCaseFieldListener) quando presente.
     */
    @UpperCase
    @Column(name = "invoice_number", length = 60)
    private String invoiceNumber;

    /**
     * Data da nota fiscal vinculada ao boleto. Opcional.
     */
    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    /**
     * Número da parcela do boleto (manual ou automático). Opcional.
     * Quando o boleto é criado via parcelamento (installmentsCount > 1),
     * este campo é preenchido automaticamente (1, 2, 3, ...). Para boletos
     * avulsos, pode ser informado manualmente pelo usuário.
     */
    @Column(name = "installment_number")
    private Integer installmentNumber;

    /**
     * Identificador do plano de parcelamento que agrupa todas as parcelas
     * geradas em um mesmo cadastro (UUID string). Nulo para boletos avulsos
     * (sem parcelamento). Permite recuperar/cancelar todas as parcelas de
     * um mesmo plano posteriormente.
     */
    @Column(name = "installment_plan_id", length = 36)
    private String installmentPlanId;

    /**
     * Inicialização antes de persistir: garante que o status seja
     * {@link RegistrationStatus#ATIVO} quando não informado. Não
     * sobrescreve valores já definidos pelo chamador.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = RegistrationStatus.ATIVO;
        }
    }
}