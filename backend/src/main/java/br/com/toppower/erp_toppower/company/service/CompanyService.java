package br.com.toppower.erp_toppower.company.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.common.util.CodeSequenceGenerator;
import br.com.toppower.erp_toppower.company.dto.CompanyCreateRequest;
import br.com.toppower.erp_toppower.company.dto.CompanyResponse;
import br.com.toppower.erp_toppower.company.dto.CompanyUpdateRequest;
import br.com.toppower.erp_toppower.company.entity.Company;
import br.com.toppower.erp_toppower.company.exception.CompanyNotFoundException;
import br.com.toppower.erp_toppower.company.exception.DuplicateCompanyCnpjException;
import br.com.toppower.erp_toppower.company.mapper.CompanyMapper;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    /** Prefixo usado no código interno das empresas (ex.: {@code EMP000001}). */
    static final String CODE_PREFIX = "EMP";

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request) {
        if (companyRepository.existsByCnpj(request.cnpj())) {
            throw new DuplicateCompanyCnpjException(request.cnpj());
        }
        Company company = CompanyMapper.toEntity(request);
        company.setCode(generateNextCode());
        Company saved = companyRepository.save(company);
        return CompanyMapper.toResponse(saved);
    }

    /**
     * Gera o próximo código sequencial no formato {@code EMP000001}, {@code EMP000002}, ...
     * consultando o maior código existente com o prefixo {@link #CODE_PREFIX}.
     * Caso não haja nenhum registro com esse prefixo, retorna {@code EMP000001}.
     */
    private String generateNextCode() {
        String maxCode = companyRepository.findMaxCodeByPrefix(CODE_PREFIX);
        return CodeSequenceGenerator.nextCode(
                maxCode, CODE_PREFIX, CodeSequenceGenerator.DEFAULT_PADDING_WIDTH);
    }

    /**
     * Retorna o próximo código que seria atribuído a uma nova empresa, sem
     * persistir nada. Útil para o frontend exibir o valor previsto no campo
     * "Código" antes do cadastro.
     */
    @Transactional(readOnly = true)
    public String getNextCode() {
        return generateNextCode();
    }

    /**
     * Lista paginada de empresas. Se {@code status} for nulo, retorna todas
     * (ativas e inativas); caso contrário filtra pelo status informado.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CompanyResponse> getAll(RegistrationStatus status, Pageable pageable) {
        Page<Company> page = (status == null)
                ? companyRepository.findAll(pageable)
                : companyRepository.findByStatus(status, pageable);
        Page<CompanyResponse> mapped = page.map(CompanyMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long id) {
        return companyRepository.findById(id)
                .map(CompanyMapper::toResponse)
                .orElseThrow(() -> new CompanyNotFoundException(id));
    }

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>Apenas {@code status} → lista todas as empresas com aquele status</li>
     *   <li>Apenas {@code query} → lista todas as empresas que dão match com o texto</li>
     *   <li>Ambos → lista todas as empresas com aquele status E que dão match com o texto</li>
     *   <li>Nenhum → lista todas as empresas (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, exige no mínimo 2 caracteres.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CompanyResponse> search(String query, RegistrationStatus status, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<CompanyResponse> mapped = companyRepository
                .searchByQuery(status, trimmed, pageable)
                .map(CompanyMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyUpdateRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        CompanyMapper.applyUpdate(company, request);
        Company saved = companyRepository.save(company);
        return CompanyMapper.toResponse(saved);
    }

    /**
     * Soft delete: não remove fisicamente o registro, apenas altera o status para INATIVO.
     * Preserva o histórico de auditoria e referências em pedidos/notas fiscais.
     */
    @Transactional
    public void softDelete(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
        company.setStatus(RegistrationStatus.INATIVO);
        companyRepository.save(company);
    }

    /**
     * Reativa uma empresa inativa, alterando o status para ATIVO.
     */
    @Transactional
    public CompanyResponse activate(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
        company.setStatus(RegistrationStatus.ATIVO);
        Company saved = companyRepository.save(company);
        return CompanyMapper.toResponse(saved);
    }
}
