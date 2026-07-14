package br.com.toppower.erp_toppower.servicetemplate.mapper;

import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateCreateRequest;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateResponse;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateUpdateRequest;
import br.com.toppower.erp_toppower.servicetemplate.entity.ServiceTemplate;

public final class ServiceTemplateMapper {

    private ServiceTemplateMapper() {
    }

    public static ServiceTemplate toEntity(ServiceTemplateCreateRequest request) {
        ServiceTemplate entity = new ServiceTemplate();
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setCategory(request.category());
        return entity;
    }

    public static ServiceTemplateResponse toResponse(ServiceTemplate entity) {
        return new ServiceTemplateResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
    }

    /**
     * Aplica atualização parcial (PATCH).
     */
    public static void applyUpdate(ServiceTemplate entity, ServiceTemplateUpdateRequest request) {
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.category() != null) {
            entity.setCategory(request.category());
        }
    }
}
