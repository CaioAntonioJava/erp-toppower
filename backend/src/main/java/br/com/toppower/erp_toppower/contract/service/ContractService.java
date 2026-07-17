package br.com.toppower.erp_toppower.contract.service;

import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.entity.Company;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.contract.dto.ContractCreateRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractUpdateRequest;
import br.com.toppower.erp_toppower.contract.dto.NextContractCodeResponse;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import br.com.toppower.erp_toppower.contract.exception.ContractBusinessException;
import br.com.toppower.erp_toppower.contract.exception.ContractClientNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.ContractNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.InvalidContractClientException;
import br.com.toppower.erp_toppower.contract.mapper.ContractMapper;
import br.com.toppower.erp_toppower.contract.repository.ContractRepository;
import br.com.toppower.erp_toppower.customer.entity.Customer;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Regras de negócio do ciclo de vida de um contrato.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>Gerar o código comercial ({@code <prefix>-<seq>-<year>}, ex.:
 *       {@code CL-001-2026} para Top Power Materiais ou
 *       {@code CT-001-2026} para Top Power Engenharia): prefixo lido da
 *       {@code Organization} ativa ({@code contract_prefix}), sequência
 *       reiniciando a {@code 1} a cada novo ano <b>por Organization</b>,
 *       ano corrente;</li>
 *   <li>Validar a invariante de cliente (exatamente um entre
 *       {@code customerId} e {@code companyId});</li>
 *   <li>Validar a existência do cliente referenciado;</li>
 *   <li>Pré-preencher o título como
 *       {@code "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código>"} quando o
 *       cliente não o informar;</li>
 *   <li>Pré-preencher a descrição com o template padrão da Organization
 *       ({@code contract_default_description}) quando o cliente não a
 *       informar e a Organization tiver um template configurado;</li>
 *   <li>CRUD completo (create, getAll, getById, search, update);</li>
 *   <li>Soft delete (inativar) e reativação.</li>
 * </ul>
 */
@Service
public class ContractService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final ContractRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;

    public ContractService(ContractRepository repository,
                           OrganizationRepository organizationRepository,
                           CustomerRepository customerRepository,
                           CompanyRepository companyRepository) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Transactional
    public ContractResponse create(ContractCreateRequest request) {
        validateClientReference(request.customerId(), request.companyId(), true);

        Contract contract = ContractMapper.toEntity(request);
        applyNextCode(contract);
        applyDefaults(contract, request);
        Contract saved = repository.save(contract);
        return toResponseWithClient(saved);
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<ContractResponse> getAll(ContractStatus status, Pageable pageable) {
        Page<Contract> page = (status == null)
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        Page<ContractResponse> mapped = page.map(this::toResponseWithClient);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public ContractResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponseWithClient)
                .orElseThrow(() -> new ContractNotFoundException(id));
    }

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>Apenas {@code status} → lista todos os contratos com aquele status</li>
     *   <li>Apenas {@code query} → lista os contratos que dão match com o texto</li>
     *   <li>Ambos → lista os contratos com aquele status E que dão match</li>
     *   <li>Nenhum → lista todos (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, exige no mínimo 2 caracteres.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ContractResponse> search(String query, ContractStatus status, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<ContractResponse> mapped = repository
                .searchByQuery(status, trimmed, pageable)
                .map(this::toResponseWithClient);
        return PagedResponse.from(mapped);
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Transactional
    public ContractResponse update(Long id, ContractUpdateRequest request) {
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));

        // Computa o cliente efetivo após o PATCH: se o request trouxe um
        // valor (inclusive null), usa o novo; senão mantém o existente.
        Long effectiveCustomer = (request.customerId() != null)
                ? request.customerId() : contract.getCustomerId();
        Long effectiveCompany = (request.companyId() != null)
                ? request.companyId() : contract.getCompanyId();
        validateClientReference(effectiveCustomer, effectiveCompany, false);

        ContractMapper.applyUpdate(contract, request);
        Contract saved = repository.save(contract);
        return toResponseWithClient(saved);
    }

    // ---------------------------------------------------------------------
    // Soft delete / Activate
    // ---------------------------------------------------------------------

    /**
     * Soft delete: não remove fisicamente o registro, apenas altera o
     * status para {@link ContractStatus#INATIVO}.
     */
    @Transactional
    public void softDelete(Long id) {
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));
        contract.setStatus(ContractStatus.INATIVO);
        repository.save(contract);
    }

    /**
     * Reativa um contrato inativo, alterando o status para
     * {@link ContractStatus#ATIVO}.
     */
    @Transactional
    public ContractResponse activate(Long id) {
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));
        contract.setStatus(ContractStatus.ATIVO);
        Contract saved = repository.save(contract);
        return toResponseWithClient(saved);
    }

    // ---------------------------------------------------------------------
    // Utilitários públicos
    // ---------------------------------------------------------------------

    /**
     * Retorna o código (e o título padrão) que seriam atribuídos ao
     * próximo contrato, sem persistir nada. Útil para o frontend exibir
     * o valor previsto no formulário antes do envio.
     *
     * <p>O prefixo vem da {@code Organization} ativa e a sequência é
     * computada de forma independente por Organization/ano.</p>
     */
    @Transactional(readOnly = true)
    public NextContractCodeResponse getNextCode() {
        Long orgId = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        String prefix = currentOrgContractPrefix(orgId);
        long sequence = generateNextSequence(year, orgId);
        String code = prefix + "-" + formatSequence(sequence) + "-" + year;
        String defaultTitle = Contract.defaultTitle(prefix, sequence, year);
        LocalDate defaultValidityDate = LocalDate.now();

        // Carrega o template de descrição padrão da Organization ativa
        String defaultDescription = organizationRepository.findById(orgId)
                .map(Organization::getContractDefaultDescription)
                .filter(d -> d != null && !d.isBlank())
                .orElse(null);

        return new NextContractCodeResponse(prefix, sequence, year, code, defaultTitle,
                defaultValidityDate, defaultDescription);
    }

    // ---------------------------------------------------------------------
    // Helpers — geração do código
    // ---------------------------------------------------------------------

    /**
     * Aplica o próximo código disponível ao contrato: prefixo da
     * {@code Organization} ativa, sequência independente por
     * Organization/ano e ano corrente.
     */
    private void applyNextCode(Contract contract) {
        Long orgId = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        long sequence = generateNextSequence(year, orgId);
        contract.setPrefix(currentOrgContractPrefix(orgId));
        contract.setSequence(sequence);
        contract.setYear(year);
    }

    private long generateNextSequence(int year, Long organizationId) {
        Long maxSequence = repository.findMaxSequenceByYearAndOrganizationId(year, organizationId);
        return (maxSequence == null) ? 1L : maxSequence + 1L;
    }

    /**
     * Resolve o prefixo de Contrato da {@code Organization} ativa
     * ({@link OrganizationContext}). Lança exceção de negócio quando a
     * Organization não tiver {@code contract_prefix} configurado.
     */
    private String currentOrgContractPrefix(Long orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ContractBusinessException(
                        "Organization ativa não encontrada: " + orgId));
        String prefix = org.getContractPrefix();
        if (prefix == null || prefix.isBlank()) {
            throw new ContractBusinessException(
                    "Organization ativa (" + org.getTradeName() + ") não possui "
                            + "contract_prefix configurado. Atualize o cadastro da "
                            + "empresa antes de emitir contratos.");
        }
        return prefix;
    }

    private static String formatSequence(long sequence) {
        return String.format("%03d", sequence);
    }

    // ---------------------------------------------------------------------
    // Helpers — defaults do backend
    // ---------------------------------------------------------------------

    /**
     * Aplica os defaults do backend quando o request não os trouxer:
     * <ul>
     *   <li>{@code title}: pré-preenchido com
     *       {@code "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código>"};</li>
     *   <li>{@code description}: pré-preenchida com o template padrão da
     *       Organization ativa ({@code contract_default_description}),
     *       quando disponível.</li>
     * </ul>
     * Não sobrescreve valores enviados pelo cliente.
     */
    private void applyDefaults(Contract contract, ContractCreateRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            contract.setTitle(Contract.defaultTitle(
                    contract.getPrefix(), contract.getSequence(), contract.getYear()));
        }
        if ((request.description() == null || request.description().isBlank())) {
            Long orgId = OrganizationContext.require();
            Organization org = organizationRepository.findById(orgId).orElse(null);
            if (org != null
                    && org.getContractDefaultDescription() != null
                    && !org.getContractDefaultDescription().isBlank()) {
                contract.setDescription(org.getContractDefaultDescription());
            }
        }
    }

    // ---------------------------------------------------------------------
    // Helpers — validação e resolução de cliente
    // ---------------------------------------------------------------------

    /**
     * Valida a invariante de cliente: exatamente um entre
     * {@code customerId} e {@code companyId} deve ser não nulo.
     *
     * @param verifyExists quando {@code true}, também verifica se o
     *                     registro referenciado existe no banco
     */
    private void validateClientReference(Long customerId, Long companyId, boolean verifyExists) {
        if (customerId == null && companyId == null) {
            throw InvalidContractClientException.bothNull();
        }
        if (customerId != null && companyId != null) {
            throw InvalidContractClientException.bothSet();
        }
        if (!verifyExists) return;

        if (customerId != null) {
            if (!customerRepository.existsById(customerId)) {
                throw new ContractClientNotFoundException("CUSTOMER", customerId);
            }
        } else {
            if (!companyRepository.existsById(companyId)) {
                throw new ContractClientNotFoundException("COMPANY", companyId);
            }
        }
    }

    /**
     * Resolve os dados de exibição do cliente (nome e código) para
     * compor a resposta.
     */
    private ClientResolved resolveClient(Contract contract) {
        if (contract.getCustomerId() != null) {
            return customerRepository.findById(contract.getCustomerId())
                    .map(c -> new ClientResolved(c.getName(), c.getCode()))
                    .orElse(ClientResolved.EMPTY);
        }
        if (contract.getCompanyId() != null) {
            return companyRepository.findById(contract.getCompanyId())
                    .map(c -> {
                        String name = (c.getTradeName() != null && !c.getTradeName().isBlank())
                                ? c.getTradeName() : c.getLegalName();
                        return new ClientResolved(name, c.getCode());
                    })
                    .orElse(ClientResolved.EMPTY);
        }
        return ClientResolved.EMPTY;
    }

    private ContractResponse toResponseWithClient(Contract contract) {
        ClientResolved client = resolveClient(contract);
        return ContractMapper.toResponse(contract, client.name(), client.code());
    }

    private record ClientResolved(String name, String code) {
        static final ClientResolved EMPTY = new ClientResolved(null, null);
    }
}