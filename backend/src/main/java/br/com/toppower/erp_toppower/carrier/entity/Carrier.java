package br.com.toppower.erp_toppower.carrier.entity;

import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.BaseEntity;
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

/**
 * Entidade que representa uma transportadora cadastrada no sistema.
 *
 * <p><b>Cadeia de herança:</b></p>
 * <pre>
 *   Carrier
 *     └─ extends BaseEntity  (campos: uuid, createdAt, updatedAt, createdBy, updatedBy)
 * </pre>
 *
 * <p>A herança de {@code BaseEntity} garante o identificador UUID e a
 * auditoria completa (createdAt, updatedAt, createdBy, updatedBy). O
 * listener {@code UpperCaseFieldListener} (registrado em {@code BaseEntity})
 * normaliza o campo {@code name} para MAIÚSCULAS em qualquer gravação,
 * independente de como o valor chegou (DTO, save direto, seed, etc.).</p>
 *
 * <p>Todos os campos de negócio são opcionais: uma transportadora pode
 * ser cadastrada apenas com o nome, apenas com o valor de frete, com
 * ambos, ou — embora não seja o uso típico — sem nenhum (caso em que
 * serve apenas como placeholder a ser complementado depois).</p>
 *
 * <p>O atributo {@code status} indica se a transportadora está ativa
 * (pode ser selecionada em operações) ou inativa. Default = ATIVO no
 * momento da persistência inicial.</p>
 */
@Entity
@Table(name = "carriers")
@Getter
@Setter
@NoArgsConstructor
public class Carrier extends BaseEntity {

    /**
     * Nome da transportadora. Opcional. Normalizado para MAIÚSCULAS
     * pelo {@code UpperCaseFieldListener} no momento da gravação.
     */
    @UpperCase
    @Column(name = "name", length = 150)
    private String name;

    /**
     * Valor padrão do frete cobrado pela transportadora. Opcional.
     * Precisão: até 99.999.999,99 (precision=10, scale=2).
     */
    @Column(name = "freight_value", precision = 10, scale = 2)
    private BigDecimal freightValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CarrierStatus status;

    /**
     * Inicialização da transportadora antes de persistir.
     * Garante que o status seja {@link CarrierStatus#ATIVO} quando não for informado.
     * Não sobrescreve valores já definidos pelo chamador.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = CarrierStatus.ATIVO;
        }
    }
}
