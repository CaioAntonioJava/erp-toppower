package br.com.toppower.erp_toppower.servicetemplate.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import br.com.toppower.erp_toppower.servicetemplate.enums.ServiceCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modelo de serviço prestado, utilizado como catálogo reutilizável em propostas
 * comerciais, propostas técnicas e pedidos de venda.
 *
 * <p>Entidade <strong>global</strong>: não herda de {@code OrganizationScopedEntity}
 * e, portanto, não é isolada por organização. Os serviços cadastrados são
 * compartilhados entre todas as empresas, servindo como um catálogo central
 * de serviços que pode ser referenciado em qualquer contexto.</p>
 */
@Entity
@Table(name = "service_templates")
@Getter
@Setter
@NoArgsConstructor
public class ServiceTemplate extends BaseEntity {

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ServiceCategory category;
}
