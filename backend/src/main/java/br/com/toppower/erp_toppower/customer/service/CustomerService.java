package br.com.toppower.erp_toppower.customer.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.common.util.CodeSequenceGenerator;
import br.com.toppower.erp_toppower.customer.dto.CustomerCreateRequest;
import br.com.toppower.erp_toppower.customer.dto.CustomerResponse;
import br.com.toppower.erp_toppower.customer.dto.CustomerUpdateRequest;
import br.com.toppower.erp_toppower.customer.entity.Customer;
import br.com.toppower.erp_toppower.customer.exception.CustomerNotFoundException;
import br.com.toppower.erp_toppower.customer.exception.DuplicateCustomerCpfException;
import br.com.toppower.erp_toppower.customer.mapper.CustomerMapper;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    /** Prefixo usado no código interno dos clientes PF (ex.: {@code CLI000001}). */
    static final String CODE_PREFIX = "CLI";

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        if (customerRepository.existsByCpf(request.cpf())) {
            throw new DuplicateCustomerCpfException(request.cpf());
        }
        Customer customer = CustomerMapper.toEntity(request);
        customer.setCode(generateNextCode());
        Customer saved = customerRepository.save(customer);
        return CustomerMapper.toResponse(saved);
    }

    /**
     * Gera o próximo código sequencial no formato {@code CLI000001}, {@code CLI000002}, ...
     * consultando o maior código existente com o prefixo {@link #CODE_PREFIX}.
     * Caso não haja nenhum registro com esse prefixo, retorna {@code CLI000001}.
     */
    private String generateNextCode() {
        String maxCode = customerRepository.findMaxCodeByPrefix(CODE_PREFIX);
        return CodeSequenceGenerator.nextCode(
                maxCode, CODE_PREFIX, CodeSequenceGenerator.DEFAULT_PADDING_WIDTH);
    }

    /**
     * Lista paginada de clientes (PF). Se {@code status} for nulo, retorna
     * todos (ativos e inativos); caso contrário filtra pelo status informado.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> getAll(RegistrationStatus status, Pageable pageable) {
        Page<Customer> page = (status == null)
                ? customerRepository.findAll(pageable)
                : customerRepository.findByStatus(status, pageable);
        Page<CustomerResponse> mapped = page.map(CustomerMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return customerRepository.findById(id)
                .map(CustomerMapper::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>Apenas {@code status} → lista todos os clientes com aquele status</li>
     *   <li>Apenas {@code query} → lista todos os clientes que dão match com o texto</li>
     *   <li>Ambos → lista os clientes com aquele status E que dão match com o texto</li>
     *   <li>Nenhum → lista todos os clientes (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, exige no mínimo 2 caracteres.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> search(String query, RegistrationStatus status, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<CustomerResponse> mapped = customerRepository
                .searchByQuery(status, trimmed, pageable)
                .map(CustomerMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        CustomerMapper.applyUpdate(customer, request);
        Customer saved = customerRepository.save(customer);
        return CustomerMapper.toResponse(saved);
    }

    /**
     * Soft delete: não remove fisicamente o registro, apenas altera o status para INATIVO.
     */
    @Transactional
    public void softDelete(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        customer.setStatus(RegistrationStatus.INATIVO);
        customerRepository.save(customer);
    }

    /**
     * Reativa um cliente inativo, alterando o status para ATIVO.
     */
    @Transactional
    public CustomerResponse activate(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        customer.setStatus(RegistrationStatus.ATIVO);
        Customer saved = customerRepository.save(customer);
        return CustomerMapper.toResponse(saved);
    }
}
