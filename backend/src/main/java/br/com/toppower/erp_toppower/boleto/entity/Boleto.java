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
 *   <li>{@code description} — descrição do boleto (obrigatório).</li>
 *   <li>{@code payee} — beneficiário (quem recebe o pagamento).</li>
 *   <li>{@code value} — valor do boleto.</li>
 *   <li>{@code dueDate} — data de vencimento.</li>
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
     * Descrição do boleto (campo obrigatório).
     * Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    /**
     * Beneficiário do boleto — quem deve receber o pagamento.
     * Opcional; salvo em MAIÚSCULAS (UpperCaseFieldListener) quando presente.
     */
    @UpperCase
    @Column(name = "payee", length = 200)
    private String payee;

    /**
     * Valor do boleto. Obrigatório.
     */
    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    /**
     * Data de vencimento do boleto. Obrigatória.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status;

    /**
     * Referência opcional ao {@code Supplier} (fornecedor) vinculado
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
     * Data em que o boleto foi liquidado. Nula enquanto não pago.
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * Nº de Contrato/Obra vinculado ao boleto (campo livre, opcional).
     * Permite associar o boleto a um contrato ou obra específica para
     * relatórios e conciliação. Salvo em MAIÚSCULAS.
     */
    @UpperCase
    @Column(name = "contract_work_number", length = 60)
    private String contractWorkNumber;

    /**
     * Data de cadastro do boleto. Distinta do {@code createdAt} (auditoria,
     * preenchido automaticamente): este campo é informável pelo usuário e
     * representa a data em que o boleto foi efetivamente registrado no
     * sistema. Default: data atual no {@code @PrePersist}.
     */
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    /**
     * Inicialização antes de persistir: garante que o status seja
     * {@link RegistrationStatus#ATIVO} e a data de cadastro seja a data
     * atual quando não informados. Não sobrescreve valores já definidos
     * pelo chamador.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = RegistrationStatus.ATIVO;
        }
        if (registrationDate == null) {
            registrationDate = LocalDate.now();
        }
    }
}