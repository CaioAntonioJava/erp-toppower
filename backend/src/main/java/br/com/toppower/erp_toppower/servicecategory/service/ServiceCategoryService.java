package br.com.toppower.erp_toppower.servicecategory.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.servicecategory.dto.ServiceCategoryCreateRequest;
import br.com.toppower.erp_toppower.servicecategory.dto.ServiceCategoryResponse;
import br.com.toppower.erp_toppower.servicecategory.dto.ServiceCategoryUpdateRequest;
import br.com.toppower.erp_toppower.servicecategory.entity.ServiceCategory;
import br.com.toppower.erp_toppower.servicecategory.enums.ServiceCategoryStatus;
import br.com.toppower.erp_toppower.servicecategory.exception.DuplicateServiceCategoryNameException;
import br.com.toppower.erp_toppower.servicecategory.exception.ServiceCategoryNotFoundException;
import br.com.toppower.erp_toppower.servicecategory.mapper.ServiceCategoryMapper;
import br.com.toppower.erp_toppower.servicecategory.repository.ServiceCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceCategoryService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final ServiceCategoryRepository serviceCategoryRepository;

    public ServiceCategoryService(ServiceCategoryRepository serviceCategoryRepository) {
        this.serviceCategoryRepository = serviceCategoryRepository;
    }

    @Transactional
    public ServiceCategoryResponse create(ServiceCategoryCreateRequest request) {
        if (serviceCategoryRepository.existsByName(request.name())) {
            throw new DuplicateServiceCategoryNameException(request.name());
        }
        ServiceCategory entity = ServiceCategoryMapper.toEntity(request);
        ServiceCategory saved = serviceCategoryRepository.save(entity);
        return ServiceCategoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ServiceCategoryResponse> getAll(ServiceCategoryStatus status, Pageable pageable) {
        Page<ServiceCategory> page = (status == null)
                ? serviceCategoryRepository.findAll(pageable)
                : serviceCategoryRepository.findByStatus(status, pageable);
        return PagedResponse.from(page.map(ServiceCategoryMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> findAllActive() {
        return serviceCategoryRepository.findAllByStatus(ServiceCategoryStatus.ATIVO)
                .stream()
                .map(ServiceCategoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceCategoryResponse getById(Long id) {
        return serviceCategoryRepository.findById(id)
                .map(ServiceCategoryMapper::toResponse)
                .orElseThrow(() -> new ServiceCategoryNotFoundException(id));
    }

    @Transactional
    public ServiceCategoryResponse update(Long id, ServiceCategoryUpdateRequest request) {
        ServiceCategory entity = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new ServiceCategoryNotFoundException(id));
        if (request.name() != null && !request.name().equals(entity.getName())) {
            if (serviceCategoryRepository.existsByNameAndIdNot(request.name(), id)) {
                throw new DuplicateServiceCategoryNameException(request.name());
            }
        }
        ServiceCategoryMapper.applyUpdate(entity, request);
        ServiceCategory saved = serviceCategoryRepository.save(entity);
        return ServiceCategoryMapper.toResponse(saved);
    }

    @Transactional
    public void softDelete(Long id) {
        ServiceCategory entity = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new ServiceCategoryNotFoundException(id));
        entity.setStatus(ServiceCategoryStatus.INATIVO);
        serviceCategoryRepository.save(entity);
    }

    @Transactional
    public ServiceCategoryResponse activate(Long id) {
        ServiceCategory entity = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new ServiceCategoryNotFoundException(id));
        entity.setStatus(ServiceCategoryStatus.ATIVO);
        ServiceCategory saved = serviceCategoryRepository.save(entity);
        return ServiceCategoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ServiceCategoryResponse> search(String query, ServiceCategoryStatus status, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<ServiceCategoryResponse> mapped = serviceCategoryRepository
                .searchByQuery(status, trimmed, pageable)
                .map(ServiceCategoryMapper::toResponse);
        return PagedResponse.from(mapped);
    }
}