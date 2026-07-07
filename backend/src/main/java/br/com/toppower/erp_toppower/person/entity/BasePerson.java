package br.com.toppower.erp_toppower.person.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.common.validation.ValidCpf;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade base com os campos compartilhados por entidades que representam
 * pessoas (físicas ou jurídicas) no sistema: nome, e-mail, telefone e CPF.
 *
 * <p>Herdar {@link BaseEntity} fornece o identificador UUID e a auditoria
 * completa (timestamps + autor).</p>
 *
 * <p>O campo {@code cpf} é validado por {@link ValidCpf} (incluindo
 * dígitos verificadores), portanto todas as entidades que herdam desta
 * classe (Profile, Seller, etc.) herdam automaticamente essa validação.</p>
 *
 * <p>O campo {@code name} é normalizado para MAIÚSCULAS pelo
 * {@code UpperCaseFieldListener} (registrado em {@link BaseEntity})
 * antes de cada persistência/atualização, portanto todos os descendentes
 * ({@code Customer}, {@code Profile}, {@code Seller}, ...) também salvam
 * o nome em maiúsculas.</p>
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class BasePerson extends OrganizationScopedEntity {

    @UpperCase
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @ValidCpf(message = "CPF inválido (dígitos verificadores incorretos ou formato incorreto)")
    @Column(name = "cpf", nullable = false, length = 14, unique = true)
    private String cpf;
}
