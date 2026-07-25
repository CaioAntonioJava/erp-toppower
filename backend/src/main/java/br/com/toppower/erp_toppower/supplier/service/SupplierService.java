package br.com.toppower.erp_toppower.supplier.service;

import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.common.embeddable.Address;
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

import java.util.Optional;

@Service
public class SupplierService {

    private static final String GENERIC_TAX_ID = "59.530.698/0001-08";

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
    public SupplierResponse getById(Long id) {
        return supplierRepository.findById(id)
                .map(SupplierMapper::toResponse)
                .orElseThrow(() -> new SupplierNotFoundException(id));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierUpdateRequest request) {
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
    public void softDelete(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(id));
        supplier.setStatus(SupplierStatus.INATIVO);
        supplierRepository.save(supplier);
    }

    @Transactional
    public SupplierResponse activate(Long id) {
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

    /**
     * Retorna o fornecedor genérico "Boleto Avulso" para a organização
     * corrente, criando-o se não existir. Usado para liquidar boletos
     * que não possuem fornecedor vinculado.
     */
    @Transactional
    public Supplier findOrCreateGeneric() {
        Optional<Supplier> existing = supplierRepository.findByTaxId(GENERIC_TAX_ID);
        if (existing.isPresent()) {
            return existing.get();
        }
        Supplier supplier = new Supplier();
        supplier.setLegalName("BOLETO AVULSO");
        supplier.setTradeName("Boleto Avulso");
        supplier.setTaxId(GENERIC_TAX_ID);
        supplier.setEmail("boleto@avulso.com");
        supplier.setPhone("(00) 0000-0000");
        supplier.setContactName("Boleto Avulso");
        Address address = new Address();
        address.setStreet("Av. Genérica");
        address.setNumber("S/N");
        address.setComplement("");
        address.setNeighborhood("Centro");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setZipCode("00000-000");
        supplier.setAddress(address);
        supplier.setStatus(SupplierStatus.ATIVO);
        return supplierRepository.save(supplier);
    }
}
