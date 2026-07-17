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
 *   <li>{@code documentNumber} — número do documento do boleto (obrigatório).</li>
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
     * Número do documento do boleto (campo obrigatório).
     * Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    /**
     * Beneficiário do boleto — quem deve receber o pagamento.
     * Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "payee", nullable = false, length = 200)
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
     * Inicialização antes de persistir: garante que o status seja
     * {@link RegistrationStatus#ATIVO} quando não for informado.
     * Não sobrescreve valores já definidos pelo chamador.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = RegistrationStatus.ATIVO;
        }
    }
}