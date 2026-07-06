package br.com.toppower.erp_toppower.tenant.service;

import br.com.toppower.erp_toppower.common.context.TenantContext;
import br.com.toppower.erp_toppower.common.util.CodeSequenceGenerator;
import br.com.toppower.erp_toppower.tenant.dto.TenantCreateRequest;
import br.com.toppower.erp_toppower.tenant.dto.TenantResponse;
import br.com.toppower.erp_toppower.tenant.dto.TenantSummary;
import br.com.toppower.erp_toppower.tenant.entity.Tenant;
import br.com.toppower.erp_toppower.tenant.exception.DuplicateTenantCnpjException;
import br.com.toppower.erp_toppower.tenant.exception.TenantNotFoundException;
import br.com.toppower.erp_toppower.tenant.mapper.TenantMapper;
import br.com.toppower.erp_toppower.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    /** Prefixo do código interno dos tenants (ex.: {@code TEN000001}). */
    static final String CODE_PREFIX = "TEN";

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Cria um novo tenant. O tenant é uma entidade <b>global</b> (não herda de
     * {@code TenantScopedEntity}), portanto a criação não depende do
     * {@link TenantContext}.
     */
    @Transactional
    public TenantResponse create(TenantCreateRequest request) {
        if (tenantRepository.existsByCnpj(request.cnpj())) {
            throw new DuplicateTenantCnpjException(request.cnpj());
        }
        Tenant tenant = TenantMapper.toEntity(request);
        tenant.setCode(generateNextCode());
        Tenant saved = tenantRepository.save(tenant);
        return TenantMapper.toResponse(saved);
    }

    private String generateNextCode() {
        String maxCode = tenantRepository.findMaxCodeByPrefix(CODE_PREFIX);
        return CodeSequenceGenerator.nextCode(
                maxCode, CODE_PREFIX, CodeSequenceGenerator.DEFAULT_PADDING_WIDTH);
    }

    @Transactional(readOnly = true)
    public String getNextCode() {
        return generateNextCode();
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAll() {
        return tenantRepository.findAll().stream()
                .map(TenantMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse getById(UUID id) {
        return tenantRepository.findById(id)
                .map(TenantMapper::toResponse)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    /**
     * Carrega a entidade {@link Tenant} por UUID. Exposto para que outros
     * serviços (auth/switch-tenant) validem o tenant sem duplicar lógica.
     */
    @Transactional(readOnly = true)
    public Tenant getEntityById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<TenantSummary> toSummaries(List<Tenant> tenants) {
        return tenants.stream().map(TenantMapper::toSummary).toList();
    }
}