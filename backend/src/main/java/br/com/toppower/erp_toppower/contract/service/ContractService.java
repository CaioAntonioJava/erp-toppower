package br.com.toppower.erp_toppower.contract.service;

import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.entity.Company;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.contract.dto.ContractClauseRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractCreateRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractProductItemRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractServiceItemRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractSummaryResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractUpdateRequest;
import br.com.toppower.erp_toppower.contract.dto.NextContractCodeResponse;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.contract.entity.ContractClause;
import br.com.toppower.erp_toppower.contract.entity.ContractProductItem;
import br.com.toppower.erp_toppower.contract.entity.ContractServiceItem;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import br.com.toppower.erp_toppower.contract.exception.ContractBusinessException;
import br.com.toppower.erp_toppower.contract.exception.ContractCompanyNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.ContractCustomerNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.ContractNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.InvalidContractClientException;
import br.com.toppower.erp_toppower.contract.mapper.ContractMapper;
import br.com.toppower.erp_toppower.contract.repository.ContractClauseRepository;
import br.com.toppower.erp_toppower.contract.repository.ContractProductItemRepository;
import br.com.toppower.erp_toppower.contract.repository.ContractRepository;
import br.com.toppower.erp_toppower.contract.repository.ContractServiceItemRepository;
import br.com.toppower.erp_toppower.customer.entity.Customer;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Regras de negócio do ciclo de vida de um contrato.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>Gerar o código comercial ({@code <prefix>-<seq>-<year>}, ex.:
 *       {@code CT-001-2026} para Top Power Engenharia ou
 *       {@code CL-001-2026} para Top Power Materiais): prefixo lido da
 *       {@code Organization} ativa ({@link OrganizationContext}), sequência
 *       reiniciando a {@code 1} a cada novo ano <b>por Organization</b>,
 *       ano corrente;</li>
 *   <li>Validar a invariante de cliente: <b>exatamente um</b> entre
 *       {@code customerUuid} (PF) e {@code companyUuid} (PJ);</li>
 *   <li>Validar a existência do cliente (PF) e da empresa (PJ)
 *       referenciados;</li>
 *   <li>Persistir o agregado e devolver o {@link ContractResponse}
 *       montado, com o nome e o código do cliente resolvidos em uma única
 *       passada;</li>
 *   <li>Listar com filtros (status, intervalo de datas, cliente, trecho
 *       do código);</li>
 *   <li>Transições de status: {@code ABERTA → EM_ANDAMENTO → CONCLUIDA}.</li>
 * </ul>
 *
 * <p>Diferente da Proposta Técnica, este agregado não possui itens
 * estruturados (serviços/produtos) — os blocos de texto opcionais
 * ({@code servicesDescription}, {@code productsDescription}) são
 * persistidos como campos {@code TEXT} diretamente no header.</p>
 */
@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractClauseRepository contractClauseRepository;
    private final ContractServiceItemRepository contractServiceItemRepository;
    private final ContractProductItemRepository contractProductItemRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final OrganizationRepository organizationRepository;

    public ContractService(ContractRepository contractRepository,
                           ContractClauseRepository contractClauseRepository,
                           ContractServiceItemRepository contractServiceItemRepository,
                           ContractProductItemRepository contractProductItemRepository,
                           CustomerRepository customerRepository,
                           CompanyRepository companyRepository,
                           OrganizationRepository organizationRepository) {
        this.contractRepository = contractRepository;
        this.contractClauseRepository = contractClauseRepository;
        this.contractServiceItemRepository = contractServiceItemRepository;
        this.contractProductItemRepository = contractProductItemRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.organizationRepository = organizationRepository;
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Transactional
    public ContractResponse create(ContractCreateRequest request) {
        validateClientReference(request.customerUuid(), request.companyUuid(), true);

        Contract header = ContractMapper.toEntity(request);
        applyNextCode(header);

        Contract saved = contractRepository.save(header);

        List<ContractClause> clauses = persistClauses(request.clauses(), saved.getUuid());
        List<ContractServiceItem> serviceItems = persistServiceItems(request.serviceItems(), saved.getUuid());
        List<ContractProductItem> productItems = persistProductItems(request.productItems(), saved.getUuid());

        ClientResolved client = resolveClient(saved.getCustomerUuid(), saved.getCompanyUuid());
        return ContractMapper.toResponse(saved, client.type(), client.name(), client.code(), clauses, serviceItems, productItems);
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ContractResponse getById(UUID id) {
        Contract c = loadContract(id);
        List<ContractClause> clauses = contractClauseRepository.findByContractUuidOrderByCreatedAtAsc(id);
        List<ContractServiceItem> serviceItems = contractServiceItemRepository.findByContractUuidOrderByCreatedAtAsc(id);
        List<ContractProductItem> productItems = contractProductItemRepository.findByContractUuidOrderByCreatedAtAsc(id);
        ClientResolved client = resolveClient(c.getCustomerUuid(), c.getCompanyUuid());
        return ContractMapper.toResponse(c, client.type(), client.name(), client.code(), clauses, serviceItems, productItems);
    }

    @Transactional(readOnly = true)
    public ContractResponse getByCode(String code) {
        ParsedCode parsed = ParsedCode.parse(code);
        Contract c = contractRepository
                .findByPrefixAndSequenceAndYear(parsed.prefix, parsed.sequence, parsed.year)
                .orElseThrow(() -> new ContractNotFoundException(code));
        UUID id = c.getUuid();
        List<ContractClause> clauses = contractClauseRepository.findByContractUuidOrderByCreatedAtAsc(id);
        List<ContractServiceItem> serviceItems = contractServiceItemRepository.findByContractUuidOrderByCreatedAtAsc(id);
        List<ContractProductItem> productItems = contractProductItemRepository.findByContractUuidOrderByCreatedAtAsc(id);
        ClientResolved client = resolveClient(c.getCustomerUuid(), c.getCompanyUuid());
        return ContractMapper.toResponse(c, client.type(), client.name(), client.code(), clauses, serviceItems, productItems);
    }

    /**
     * Lista paginada com filtros opcionais. Todos os parâmetros são
     * opcionais; nulos significam "sem filtro".
     *
     * @param status       status do contrato (opcional)
     * @param startDate    data de início a partir de (opcional)
     * @param endDate      data de início até (opcional)
     * @param clientUuid   UUID do cliente (PF ou PJ) (opcional)
     * @param codeLike     trecho do código (opcional)
     * @param pageable     paginação e ordenação
     */
    @Transactional(readOnly = true)
    public PagedResponse<ContractSummaryResponse> search(ContractStatus status,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        UUID clientUuid,
                                                        String codeLike,
                                                        Pageable pageable) {
        Specification<Contract> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endDate));
            }
            if (clientUuid != null) {
                // match em qualquer um dos dois campos (PF ou PJ).
                predicates.add(cb.or(
                        cb.equal(root.get("customerUuid"), clientUuid),
                        cb.equal(root.get("companyUuid"), clientUuid)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Contract> page = contractRepository.findAll(spec, pageable);

        if (codeLike == null || codeLike.isBlank()) {
            Page<ContractSummaryResponse> mapped = page.map(c -> {
                ClientResolved client = resolveClient(c.getCustomerUuid(), c.getCompanyUuid());
                return ContractMapper.toSummary(c, client.type(), client.name(), client.code());
            });
            return PagedResponse.from(mapped);
        }

        // Filtro por trecho do código é aplicado em memória (código é
        // composto por prefix + sequence + year e só é formatado em
        // memória pelo formattedCode()).
        String needle = codeLike.toLowerCase().trim();
        List<ContractSummaryResponse> filtered = page.stream()
                .filter(c -> c.formattedCode().toLowerCase().contains(needle))
                .map(c -> {
                    ClientResolved client = resolveClient(c.getCustomerUuid(), c.getCompanyUuid());
                    return ContractMapper.toSummary(c, client.type(), client.name(), client.code());
                })
                .toList();
        boolean isLast = filtered.size() < pageable.getPageSize();
        return new PagedResponse<>(
                filtered,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                filtered.size(),
                isLast ? 1 : 2,
                pageable.getPageNumber() == 0,
                isLast);
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Transactional
    public ContractResponse update(UUID id, ContractUpdateRequest request) {
        Contract c = loadContract(id);

        if (c.getStatus() == ContractStatus.CONCLUIDA) {
            throw new ContractBusinessException(
                    "Contrato CONCLUIDO não pode ser alterado.");
        }

        // Recalcula o cliente efetivo após o PATCH e valida.
        UUID effectiveCustomer = (request.customerUuid() != null)
                ? request.customerUuid() : c.getCustomerUuid();
        UUID effectiveCompany = (request.companyUuid() != null)
                ? request.companyUuid() : c.getCompanyUuid();
        // Só revalida se algum dos dois foi alterado.
        if (request.customerUuid() != null || request.companyUuid() != null) {
            validateClientReference(effectiveCustomer, effectiveCompany, true);
        }

        ContractMapper.applyUpdate(c, request);
        Contract saved = contractRepository.save(c);

        // Substituição completa da lista de cláusulas, quando enviada
        List<ContractClause> clauses;
        if (request.clauses() != null) {
            contractClauseRepository.deleteByContractUuid(id);
            contractClauseRepository.flush();
            clauses = persistClauses(request.clauses(), saved.getUuid());
        } else {
            clauses = contractClauseRepository.findByContractUuidOrderByCreatedAtAsc(id);
        }

        // Substituição completa da lista de itens de serviço, quando enviada
        List<ContractServiceItem> serviceItems;
        if (request.serviceItems() != null) {
            contractServiceItemRepository.deleteByContractUuid(id);
            contractServiceItemRepository.flush();
            serviceItems = persistServiceItems(request.serviceItems(), saved.getUuid());
        } else {
            serviceItems = contractServiceItemRepository.findByContractUuidOrderByCreatedAtAsc(id);
        }

        // Substituição completa da lista de itens de produto, quando enviada
        List<ContractProductItem> productItems;
        if (request.productItems() != null) {
            contractProductItemRepository.deleteByContractUuid(id);
            contractProductItemRepository.flush();
            productItems = persistProductItems(request.productItems(), saved.getUuid());
        } else {
            productItems = contractProductItemRepository.findByContractUuidOrderByCreatedAtAsc(id);
        }

        ClientResolved client = resolveClient(saved.getCustomerUuid(), saved.getCompanyUuid());
        return ContractMapper.toResponse(saved, client.type(), client.name(), client.code(), clauses, serviceItems, productItems);
    }

    // ---------------------------------------------------------------------
    // Transições de status
    // ---------------------------------------------------------------------

    /**
     * Inicia a execução: {@code ABERTA → EM_ANDAMENTO}.
     */
    @Transactional
    public ContractResponse start(UUID id) {
        Contract c = loadContract(id);
        if (c.getStatus() != ContractStatus.ABERTA) {
            throw new ContractBusinessException(
                    "Apenas contratos ABERTA podem ser iniciados. Status atual: " + c.getStatus());
        }
        c.setStatus(ContractStatus.EM_ANDAMENTO);
        Contract saved = contractRepository.save(c);
        return buildFullResponse(saved);
    }

    /**
     * Conclui o contrato: {@code EM_ANDAMENTO → CONCLUIDA}.
     */
    @Transactional
    public ContractResponse complete(UUID id) {
        Contract c = loadContract(id);
        if (c.getStatus() != ContractStatus.EM_ANDAMENTO) {
            throw new ContractBusinessException(
                    "Apenas contratos EM_ANDAMENTO podem ser concluídos. Status atual: "
                            + c.getStatus());
        }
        c.setStatus(ContractStatus.CONCLUIDA);
        Contract saved = contractRepository.save(c);
        return buildFullResponse(saved);
    }

    /**
     * Reabre um contrato CONCLUIDO, voltando-o para EM_ANDAMENTO. Útil
     * para corrigir conclusões indevidas.
     */
    @Transactional
    public ContractResponse reopen(UUID id) {
        Contract c = loadContract(id);
        if (c.getStatus() != ContractStatus.CONCLUIDA) {
            throw new ContractBusinessException(
                    "Apenas contratos CONCLUIDA podem ser reabertos. Status atual: "
                            + c.getStatus());
        }
        c.setStatus(ContractStatus.EM_ANDAMENTO);
        Contract saved = contractRepository.save(c);
        return buildFullResponse(saved);
    }

    // ---------------------------------------------------------------------
    // Próximo código (preview)
    // ---------------------------------------------------------------------

    /**
     * Retorna o código que seria atribuído ao próximo contrato, sem
     * persistir nada. Útil para o frontend exibir o valor previsto no
     * formulário antes do envio.
     *
     * <p>O prefixo vem da {@code Organization} ativa ({@code contractPrefix}
     * lido da {@link OrganizationContext}) e a sequência é computada de
     * forma independente por Organization/ano.</p>
     */
    @Transactional(readOnly = true)
    public NextContractCodeResponse getNextCode() {
        UUID orgUuid = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        String prefix = currentOrgContractPrefix();
        long sequence = generateNextSequence(year, orgUuid);
        String code = prefix + "-" + formatSequence(sequence) + "-" + year;
        return new NextContractCodeResponse(prefix, sequence, year, code);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Contract loadContract(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));
    }

    /**
     * Aplica o próximo código disponível ao contrato: prefixo da
     * {@code Organization} ativa, sequência independente por
     * Organization/ano e ano corrente.
     */
    private void applyNextCode(Contract c) {
        UUID orgUuid = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        long sequence = generateNextSequence(year, orgUuid);
        c.setPrefix(currentOrgContractPrefix());
        c.setSequence(sequence);
        c.setYear(year);
    }

    private long generateNextSequence(int year, UUID organizationUuid) {
        Long maxSequence = contractRepository.findMaxSequenceByYearAndOrganizationUuid(year, organizationUuid);
        return (maxSequence == null) ? 1L : maxSequence + 1L;
    }

    /**
     * Resolve o prefixo de Contrato da {@code Organization} ativa
     * ({@link OrganizationContext}). Lança exceção de negócio quando a
     * Organization não tiver {@code contractPrefix} configurado
     * (situação que não deve ocorrer com o cadastro obrigatório +
     * migration V29, mas é tratada explicitamente para falhar de forma
     * clara em vez de gerar um código inválido como "-001-2026").
     */
    private String currentOrgContractPrefix() {
        UUID orgUuid = OrganizationContext.require();
        Organization org = organizationRepository.findById(orgUuid)
                .orElseThrow(() -> new ContractBusinessException(
                        "Organization ativa não encontrada: " + orgUuid));
        String prefix = org.getContractPrefix();
        if (prefix == null || prefix.isBlank()) {
            throw new ContractBusinessException(
                    "Organization ativa (" + org.getTradeName() + ") não possui "
                            + "contractPrefix configurado. Atualize o cadastro da "
                            + "empresa antes de emitir contratos.");
        }
        return prefix;
    }

    private static String formatSequence(long sequence) {
        return String.format("%03d", sequence);
    }

    /**
     * Persiste a lista de cláusulas de um contrato, associando cada uma
     * ao UUID do contrato informado. Retorna a lista de entidades
     * recém-persistidas (com UUIDs gerados).
     */
    private List<ContractClause> persistClauses(List<ContractClauseRequest> requests, UUID contractUuid) {
        return requests.stream()
                .map(req -> contractClauseRepository.save(
                        ContractMapper.toClauseEntity(req, contractUuid)))
                .toList();
    }

    /**
     * Persiste a lista de itens de serviço de um contrato, associando
     * cada um ao UUID do contrato informado. Retorna a lista de entidades
     * recém-persistidas (com UUIDs gerados).
     */
    private List<ContractServiceItem> persistServiceItems(
            List<ContractServiceItemRequest> requests, UUID contractUuid) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(req -> contractServiceItemRepository.save(
                        ContractMapper.toServiceItemEntity(req, contractUuid)))
                .toList();
    }

    /**
     * Persiste a lista de itens de produto de um contrato, associando
     * cada um ao UUID do contrato informado. Retorna a lista de entidades
     * recém-persistidas (com UUIDs gerados).
     */
    private List<ContractProductItem> persistProductItems(
            List<ContractProductItemRequest> requests, UUID contractUuid) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(req -> contractProductItemRepository.save(
                        ContractMapper.toProductItemEntity(req, contractUuid)))
                .toList();
    }

    /**
     * Constrói a resposta completa a partir da entidade, carregando todos
     * os agregados (cláusulas, itens de serviço, itens de produto).
     */
    private ContractResponse buildFullResponse(Contract c) {
        UUID id = c.getUuid();
        List<ContractClause> clauses = contractClauseRepository.findByContractUuidOrderByCreatedAtAsc(id);
        List<ContractServiceItem> serviceItems = contractServiceItemRepository.findByContractUuidOrderByCreatedAtAsc(id);
        List<ContractProductItem> productItems = contractProductItemRepository.findByContractUuidOrderByCreatedAtAsc(id);
        ClientResolved client = resolveClient(c.getCustomerUuid(), c.getCompanyUuid());
        return ContractMapper.toResponse(c, client.type(), client.name(), client.code(), clauses, serviceItems, productItems);
    }

    /**
     * Valida a invariante do cliente: exatamente um entre
     * {@code customerUuid} e {@code companyUuid} deve estar preenchido.
     * Quando {@code verifyExists} é verdadeiro (criação/PATCH
     * alterando o cliente), também verifica a existência do registro
     * referenciado.
     */
    private void validateClientReference(UUID customerUuid, UUID companyUuid, boolean verifyExists) {
        if (customerUuid == null && companyUuid == null) {
            throw InvalidContractClientException.bothNull();
        }
        if (customerUuid != null && companyUuid != null) {
            throw InvalidContractClientException.bothSet();
        }
        if (!verifyExists) {
            return;
        }
        if (customerUuid != null && !customerRepository.existsById(customerUuid)) {
            throw new ContractCustomerNotFoundException(customerUuid);
        }
        if (companyUuid != null && !companyRepository.existsById(companyUuid)) {
            throw new ContractCompanyNotFoundException(companyUuid);
        }
    }

    /**
     * Resolve o nome e o código de exibição do cliente (PF ou PJ)
     * referenciado pelo contrato. Retorna {@code (null, null)} quando o
     * registro não existe mais, mantendo o UUID como referência no DTO.
     */
    private ClientResolved resolveClient(UUID customerUuid, UUID companyUuid) {
        if (customerUuid != null) {
            Customer c = customerRepository.findById(customerUuid).orElse(null);
            if (c != null) {
                return new ClientResolved(
                        ContractResponse.ClientType.CUSTOMER,
                        c.getName(), c.getCode());
            }
            return new ClientResolved(ContractResponse.ClientType.CUSTOMER, null, null);
        }
        if (companyUuid != null) {
            Company c = companyRepository.findById(companyUuid).orElse(null);
            if (c != null) {
                String name = c.getTradeName() != null && !c.getTradeName().isBlank()
                        ? c.getTradeName()
                        : c.getLegalName();
                return new ClientResolved(ContractResponse.ClientType.COMPANY, name, c.getCode());
            }
            return new ClientResolved(ContractResponse.ClientType.COMPANY, null, null);
        }
        return ClientResolved.EMPTY;
    }

    /**
     * Trio resolvido a partir do cliente (PF ou PJ) referenciado pelo
     * contrato: tipo, nome e código.
     */
    private record ClientResolved(ContractResponse.ClientType type, String name, String code) {
        static final ClientResolved EMPTY =
                new ClientResolved(null, null, null);
    }

    /**
     * Parser do código formatado ({@code CT-001-2026}) nos campos
     * persistidos.
     */
    private record ParsedCode(String prefix, Long sequence, Integer year) {
        static ParsedCode parse(String code) {
            if (code == null || code.isBlank()) {
                throw new ContractBusinessException("Código inválido: " + code);
            }
            String[] parts = code.trim().split("-");
            if (parts.length != 3) {
                throw new ContractBusinessException(
                        "Código deve estar no formato CT-001-2026: " + code);
            }
            try {
                return new ParsedCode(
                        parts[0].toUpperCase(),
                        Long.parseLong(parts[1]),
                        Integer.parseInt(parts[2]));
            } catch (NumberFormatException ex) {
                throw new ContractBusinessException(
                        "Código deve estar no formato CT-001-2026: " + code);
            }
        }
    }

    /**
     * Expõe a entidade resolvida para uso por outros serviços do mesmo
     * módulo (ex.: {@code ContractPdfService}).
     */
    @Transactional(readOnly = true)
    public Contract getEntityById(UUID id) {
        return loadContract(id);
    }
}