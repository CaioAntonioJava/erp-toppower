package br.com.toppower.erp_toppower.person.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade base com os campos compartilhados por entidades que representam
 * pessoas (físicas ou jurídicas) no sistema: nome, e-mail, telefone e CPF.
 *
 * <p>Herdar {@link BaseEntity} garante também o identificador UUID e a
 * auditoria completa (timestamps + autor).</p>
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class BasePerson extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "cpf", nullable = false, length = 14, unique = true)
    private String cpf;
}
