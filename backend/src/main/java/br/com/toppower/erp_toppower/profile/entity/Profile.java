package br.com.toppower.erp_toppower.profile.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import br.com.toppower.erp_toppower.common.validation.ValidCpf;
import br.com.toppower.erp_toppower.profile.enums.ProfileStatus;
import br.com.toppower.erp_toppower.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade que representa o perfil (dados pessoais e profissionais) de um
 * usuário do sistema. Cada {@link Profile} está vinculado a exatamente um
 * {@link User} — aquele que o cadastrou — por meio de um relacionamento
 * 1:1 unidirecional.
 *
 * <p><b>Entidade global (não tenant-scoped).</b> Diferente de Customer,
 * Seller e demais entidades de negócio, o perfil pertence ao <i>usuário</i>,
 * não à empresa (tenant) em que ele está operando no momento. Um usuário
 * vinculado a múltiplas empresas preenche o perfil uma única vez; ao
 * alternar entre tenants via switch-tenant, o mesmo perfil continua
 * visível — sem duplicidade de CPF/dados pessoais.</p>
 *
 * <p>Por isso {@link Profile} herda diretamente de {@link BaseEntity}
 * (UUID + auditoria) e <strong>não</strong> de {@code TenantScopedEntity}:
 * não há coluna {@code tenant_uuid} nem filtro Hibernate por tenant. Os
 * campos de pessoa (name, email, phone, cpf) são declarados localmente,
 * espelhando {@code BasePerson}, mas sem o vínculo de tenant.</p>
 *
 * <p>O campo {@code cpf} é validado por {@link ValidCpf} (incluindo
 * dígitos verificadores) e o {@code name} é normalizado para MAIÚSCULAS
 * pelo {@code UpperCaseFieldListener} (registrado em {@link BaseEntity})
 * antes de cada persistência/atualização.</p>
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile extends BaseEntity {

    /**
     * Usuário responsável pelo cadastro deste perfil.
     * O índice único em {@code user_id} assegura a cardinalidade 1:1
     * (um User pode estar vinculado a no máximo um Profile).
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProfileStatus status;

    /**
     * Inicialização do perfil antes de persistir.
     * Garante que o status seja {@link ProfileStatus#ATIVO} quando não for informado.
     * Não sobrescreve valores já definidos pelo chamador.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = ProfileStatus.ATIVO;
        }
    }
}