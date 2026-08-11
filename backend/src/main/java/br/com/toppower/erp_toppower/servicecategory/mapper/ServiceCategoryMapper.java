package br.com.toppower.erp_toppower.servicecategory.mapper;

import br.com.toppower.erp_toppower.servicecategory.dto.ServiceCategoryCreateRequest;
import br.com.toppower.erp_toppower.servicecategory.dto.ServiceCategoryResponse;
import br.com.toppower.erp_toppower.servicecategory.dto.ServiceCategoryUpdateRequest;
import br.com.toppower.erp_toppower.servicecategory.entity.ServiceCategory;

public final class ServiceCategoryMapper {

    private ServiceCategoryMapper() {
    }

    /**
     * O {@code status} pode ser {@code null}; o {@code @PrePersist} da entidade
     * cuida de aplicar o default {@code ATIVO}.
     */
    public static ServiceCategory toEntity(ServiceCategoryCreateRequest request) {
        ServiceCategory entity = new ServiceCategory();
        entity.setName(request.name());
        entity.setStatus(request.status());
        return entity;
    }

    public static ServiceCategoryResponse toResponse(ServiceCategory entity) {
        return new ServiceCategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
    }

    /**
     * Aplica atualização parcial (PATCH).
     */
    public static void applyUpdate(ServiceCategory entity, ServiceCategoryUpdateRequest request) {
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
    }
}