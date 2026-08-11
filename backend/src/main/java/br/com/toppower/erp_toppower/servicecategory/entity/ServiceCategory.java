package br.com.toppower.erp_toppower.servicecategory.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import br.com.toppower.erp_toppower.servicecategory.enums.ServiceCategoryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Categoria de serviço do catálogo (ex.: SPDA, INSTALAÇÃO ELÉTRICA).
 *
 * <p>Entidade <strong>global</strong>: não herda de {@code
 * OrganizationScopedEntity} e, portanto, não é isolada por organização.
 * As categorias cadastradas são compartilhadas entre todas as empresas,
 * servindo como domínio de referência para classificar os
 * {@code ServiceTemplate}.</p>
 *
 * <p>Mapeada para a tabela {@code categories} (já existente no banco),
 * cuja coluna {@code name} tem length 100.</p>
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class ServiceCategory extends BaseEntity {

    @UpperCase
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServiceCategoryStatus status;

    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = ServiceCategoryStatus.ATIVO;
        }
    }
}