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
        entity.setCategoryId(request.categoryId());
        return entity;
    }

    /**
     * Converte a entidade para resposta, resolvendo o nome da categoria.
     *
     * @param entity       entidade persistida
     * @param categoryName nome da categoria resolvido via ServiceCategoryRepository
     */
    public static ServiceTemplateResponse toResponse(ServiceTemplate entity, String categoryName) {
        return new ServiceTemplateResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategoryId(),
                categoryName,
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
        if (request.categoryId() != null) {
            entity.setCategoryId(request.categoryId());
        }
    }
}