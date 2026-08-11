package br.com.toppower.erp_toppower.servicetemplate.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.servicecategory.entity.ServiceCategory;
import br.com.toppower.erp_toppower.servicecategory.exception.ServiceCategoryNotFoundException;
import br.com.toppower.erp_toppower.servicecategory.repository.ServiceCategoryRepository;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateCreateRequest;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateResponse;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateUpdateRequest;
import br.com.toppower.erp_toppower.servicetemplate.entity.ServiceTemplate;
import br.com.toppower.erp_toppower.servicetemplate.exception.ServiceTemplateNotFoundException;
import br.com.toppower.erp_toppower.servicetemplate.mapper.ServiceTemplateMapper;
import br.com.toppower.erp_toppower.servicetemplate.repository.ServiceTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class ServiceTemplateService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final ServiceTemplateRepository serviceTemplateRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;

    public ServiceTemplateService(ServiceTemplateRepository serviceTemplateRepository,
                                  ServiceCategoryRepository serviceCategoryRepository) {
        this.serviceTemplateRepository = serviceTemplateRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
    }

    @Transactional
    public ServiceTemplateResponse create(ServiceTemplateCreateRequest request) {
        validateCategoryExists(request.categoryId());
        ServiceTemplate entity = ServiceTemplateMapper.toEntity(request);
        ServiceTemplate saved = serviceTemplateRepository.save(entity);
        return ServiceTemplateMapper.toResponse(saved, resolveCategoryName(saved.getCategoryId()));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ServiceTemplateResponse> getAll(Pageable pageable) {
        Page<ServiceTemplate> page = serviceTemplateRepository.findAll(pageable);
        Map<Long, String> categoryNames = resolveCategoryNames(page);
        return PagedResponse.from(page.map(s -> ServiceTemplateMapper.toResponse(s, categoryNames.get(s.getCategoryId()))));
    }

    @Transactional(readOnly = true)
    public ServiceTemplateResponse getById(Long id) {
        return serviceTemplateRepository.findById(id)
                .map(s -> ServiceTemplateMapper.toResponse(s, resolveCategoryName(s.getCategoryId())))
                .orElseThrow(() -> new ServiceTemplateNotFoundException(id));
    }

    @Transactional
    public ServiceTemplateResponse update(Long id, ServiceTemplateUpdateRequest request) {
        ServiceTemplate entity = serviceTemplateRepository.findById(id)
                .orElseThrow(() -> new ServiceTemplateNotFoundException(id));
        if (request.categoryId() != null) {
            validateCategoryExists(request.categoryId());
        }
        ServiceTemplateMapper.applyUpdate(entity, request);
        ServiceTemplate saved = serviceTemplateRepository.save(entity);
        return ServiceTemplateMapper.toResponse(saved, resolveCategoryName(saved.getCategoryId()));
    }

    @Transactional
    public void delete(Long id) {
        if (!serviceTemplateRepository.existsById(id)) {
            throw new ServiceTemplateNotFoundException(id);
        }
        serviceTemplateRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ServiceTemplateResponse> search(String query, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<ServiceTemplate> page = serviceTemplateRepository.searchByQuery(trimmed, pageable);
        Map<Long, String> categoryNames = resolveCategoryNames(page);
        return PagedResponse.from(page.map(s -> ServiceTemplateMapper.toResponse(s, categoryNames.get(s.getCategoryId()))));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ServiceTemplateResponse> getByCategory(Long categoryId, Pageable pageable) {
        validateCategoryExists(categoryId);
        Page<ServiceTemplate> page = serviceTemplateRepository.findByCategoryId(categoryId, pageable);
        String categoryName = resolveCategoryName(categoryId);
        return PagedResponse.from(page.map(s -> ServiceTemplateMapper.toResponse(s, categoryName)));
    }

    /**
     * Valida que a categoria informada existe no banco.
     */
    private void validateCategoryExists(Long categoryId) {
        if (!serviceCategoryRepository.existsById(categoryId)) {
            throw new ServiceCategoryNotFoundException(categoryId);
        }
    }

    /**
     * Resolve o nome de uma única categoria.
     */
    private String resolveCategoryName(Long categoryId) {
        return serviceCategoryRepository.findById(categoryId)
                .map(ServiceCategory::getName)
                .orElse(null);
    }

    /**
     * Resolve em lote os nomes das categorias referenciadas em uma página
     * de ServiceTemplate, evitando N+1 consultas.
     */
    private Map<Long, String> resolveCategoryNames(Page<ServiceTemplate> page) {
        Set<Long> categoryIds = new HashSet<>();
        for (ServiceTemplate s : page.getContent()) {
            if (s.getCategoryId() != null) {
                categoryIds.add(s.getCategoryId());
            }
        }
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        serviceCategoryRepository.findAllById(categoryIds)
                .forEach(c -> result.put(c.getId(), c.getName()));
        return result;
    }
}