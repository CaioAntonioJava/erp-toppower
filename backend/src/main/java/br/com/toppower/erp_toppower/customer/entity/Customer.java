package br.com.toppower.erp_toppower.customer.entity;

import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.person.entity.BasePerson;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade que representa um cliente pessoa física.
 *
 * <p>Herdar {@link BasePerson} fornece os campos compartilhados com outras
 * pessoas do sistema (nome, e-mail, telefone, CPF) e a auditoria completa
 * vinda de {@link br.com.toppower.erp_toppower.common.entity.BaseEntity}.</p>
 *
 * <p>O CPF (herdado de {@link BasePerson}) já é validado com dígitos
 * verificadores e é único no banco.</p>
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer extends BasePerson {

    /**
     * Código interno único do cliente (ex: "CUS-001").
     * Obrigatório e único.
     */
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    /**
     * Endereço do cliente. Os campos do {@link Address} são embutidos
     * nesta mesma tabela com o prefixo {@code address_} para evitar
     * conflito com colunas próprias da entidade.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "address_street", nullable = false, length = 200)),
            @AttributeOverride(name = "number", column = @Column(name = "address_number", nullable = false, length = 20)),
            @AttributeOverride(name = "complement", column = @Column(name = "address_complement", length = 100)),
            @AttributeOverride(name = "neighborhood", column = @Column(name = "address_neighborhood", length = 100)),
            @AttributeOverride(name = "city", column = @Column(name = "address_city", nullable = false, length = 100)),
            @AttributeOverride(name = "state", column = @Column(name = "address_state", nullable = false, length = 2)),
            @AttributeOverride(name = "zipCode", column = @Column(name = "address_zip_code", nullable = false, length = 9))
    })
    private Address address;

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
