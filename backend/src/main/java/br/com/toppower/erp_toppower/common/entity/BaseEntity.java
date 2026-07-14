package br.com.toppower.erp_toppower.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import br.com.toppower.erp_toppower.common.listener.UpperCaseFieldListener;

import java.time.Instant;

/**
 * Entidade base com os campos compartilhados por todas as entidades do sistema:
 * identificador Long (auto-incremento) e auditoria completa (timestamps + e-mail do autor).
 *
 * <p>Aplica automaticamente dois listeners JPA em todas as subclasses:</p>
 * <ul>
 *   <li>{@code AuditingEntityListener} — preenche createdAt/updatedAt/createdBy/updatedBy</li>
 *   <li>{@link UpperCaseFieldListener} — normaliza para MAIÚSCULAS
 *       todos os campos {@code String} anotados com {@code @UpperCase}
 *       (declarados na entidade ou em qualquer {@code @MappedSuperclass}
 *       da hierarquia, ex: {@code BasePerson.name})</li>
 * </ul>
 *
 * <p>Requer {@code @EnableJpaAuditing(auditorAwareRef = "auditorAware")} no bootstrap.</p>
 */
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, UpperCaseFieldListener.class})
@Getter
@NoArgsConstructor
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter
    private Long id;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
