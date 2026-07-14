package br.com.toppower.erp_toppower.servicetemplate.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
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

@Service
public class ServiceTemplateService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final ServiceTemplateRepository serviceTemplateRepository;

    public ServiceTemplateService(ServiceTemplateRepository serviceTemplateRepository) {
        this.serviceTemplateRepository = serviceTemplateRepository;
    }

    @Transactional
    public ServiceTemplateResponse create(ServiceTemplateCreateRequest request) {
        ServiceTemplate entity = ServiceTemplateMapper.toEntity(request);
        ServiceTemplate saved = serviceTemplateRepository.save(entity);
        return ServiceTemplateMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ServiceTemplateResponse> getAll(Pageable pageable) {
        Page<ServiceTemplate> page = serviceTemplateRepository.findAll(pageable);
        return PagedResponse.from(page.map(ServiceTemplateMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ServiceTemplateResponse getById(Long id) {
        return serviceTemplateRepository.findById(id)
                .map(ServiceTemplateMapper::toResponse)
                .orElseThrow(() -> new ServiceTemplateNotFoundException(id));
    }

    @Transactional
    public ServiceTemplateResponse update(Long id, ServiceTemplateUpdateRequest request) {
        ServiceTemplate entity = serviceTemplateRepository.findById(id)
                .orElseThrow(() -> new ServiceTemplateNotFoundException(id));
        ServiceTemplateMapper.applyUpdate(entity, request);
        ServiceTemplate saved = serviceTemplateRepository.save(entity);
        return ServiceTemplateMapper.toResponse(saved);
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
        Page<ServiceTemplateResponse> mapped = serviceTemplateRepository
                .searchByQuery(trimmed, pageable)
                .map(ServiceTemplateMapper::toResponse);
        return PagedResponse.from(mapped);
    }
}
