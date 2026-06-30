package br.com.toppower.erp_toppower.supplier.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.supplier.dto.SupplierCreateRequest;
import br.com.toppower.erp_toppower.supplier.dto.SupplierResponse;
import br.com.toppower.erp_toppower.supplier.dto.SupplierUpdateRequest;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.enums.SupplierStatus;
import br.com.toppower.erp_toppower.supplier.exception.DuplicateSupplierCnpjException;
import br.com.toppower.erp_toppower.supplier.exception.SupplierNotFoundException;
import br.com.toppower.erp_toppower.supplier.mapper.SupplierMapper;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public SupplierResponse create(SupplierCreateRequest request) {
        if (supplierRepository.existsByTaxId(request.taxId())) {
            throw new DuplicateSupplierCnpjException(request.taxId());
        }
        Supplier supplier = SupplierMapper.toEntity(request);
        Supplier saved = supplierRepository.save(supplier);
        return SupplierMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SupplierResponse> getAll(SupplierStatus status, Pageable pageable) {
        Page<Supplier> page = (status == null)
                ? supplierRepository.findAll(pageable)
                : supplierRepository.findByStatus(status, pageable);
        Page<SupplierResponse> mapped = page.map(SupplierMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(UUID id) {
        return supplierRepository.findById(id)
                .map(SupplierMapper::toResponse)
                .orElseThrow(() -> new SupplierNotFoundException(id));
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierUpdateRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(id));

        if (request.email() != null && !request.email().equals(supplier.getEmail())) {
            if (supplierRepository.existsByEmail(request.email())) {
                throw new DuplicateSupplierCnpjException(request.email());
            }
        }

        SupplierMapper.applyUpdate(supplier, request);
        Supplier saved = supplierRepository.save(supplier);
        return SupplierMapper.toResponse(saved);
    }

    @Transactional
    public void softDelete(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(id));
        supplier.setStatus(SupplierStatus.INATIVO);
        supplierRepository.save(supplier);
    }

    @Transactional
    public SupplierResponse activate(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(id));
        supplier.setStatus(SupplierStatus.ATIVO);
        Supplier saved = supplierRepository.save(supplier);
        return SupplierMapper.toResponse(saved);
    }

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    @Transactional(readOnly = true)
    public PagedResponse<SupplierResponse> search(String query, SupplierStatus status, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<SupplierResponse> mapped = supplierRepository
                .searchByQuery(status, trimmed, pageable)
                .map(SupplierMapper::toResponse);
        return PagedResponse.from(mapped);
    }
}
