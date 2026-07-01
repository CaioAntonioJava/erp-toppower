package br.com.toppower.erp_toppower.profile.entity;

import br.com.toppower.erp_toppower.person.entity.BasePerson;
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
 * <p>Herdar {@link BasePerson} garante os campos compartilhados
 * (name, email, phone, cpf). A herança em cadeia inclui também
 * {@code BaseEntity}, que fornece identificador UUID e auditoria
 * (createdAt, updatedAt, createdBy, updatedBy).</p>
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile extends BasePerson {

    /**
     * Usuário responsável pelo cadastro deste perfil.
     * O índice único em {@code user_id} assegura a cardinalidade 1:1
     * (um User pode estar vinculado a no máximo um Profile).
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

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
