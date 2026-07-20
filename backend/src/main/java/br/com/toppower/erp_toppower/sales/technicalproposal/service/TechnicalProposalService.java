package br.com.toppower.erp_toppower.sales.technicalproposal.service;

import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.carrier.repository.CarrierRepository;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import br.com.toppower.erp_toppower.product.repository.ProductRepository;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.NextTechnicalProposalCodeResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalConditionRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalCreateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalProductItemRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalServiceItemRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSimulateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSimulateResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSummaryResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalUpdateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposal;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalCondition;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalProductItem;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalServiceItem;
import br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus;
import br.com.toppower.erp_toppower.sales.technicalproposal.exception.InvalidTechnicalProposalClientException;
import br.com.toppower.erp_toppower.sales.technicalproposal.exception.TechnicalProposalBusinessException;
import br.com.toppower.erp_toppower.sales.technicalproposal.exception.TechnicalProposalClientNotFoundException;
import br.com.toppower.erp_toppower.sales.technicalproposal.exception.TechnicalProposalNotFoundException;
import br.com.toppower.erp_toppower.sales.technicalproposal.mapper.TechnicalProposalMapper;
import br.com.toppower.erp_toppower.sales.technicalproposal.repository.TechnicalProposalConditionRepository;
import br.com.toppower.erp_toppower.sales.technicalproposal.repository.TechnicalProposalProductItemRepository;
import br.com.toppower.erp_toppower.sales.technicalproposal.repository.TechnicalProposalRepository;
import br.com.toppower.erp_toppower.sales.technicalproposal.repository.TechnicalProposalServiceItemRepository;
import br.com.toppower.erp_toppower.receivable.repository.ReceivableRepository;
import br.com.toppower.erp_toppower.receivable.service.ReceivableService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negócio do ciclo de vida de uma proposta técnica.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>Gerar o código comercial ({@code <prefix>-<seq>-<year>}, ex.:
 *       {@code PT-001-2026} ou {@code PL-001-2026}): prefixo lido da
 *       {@code Organization} ativa ({@link OrganizationContext}),
 *       sequência reiniciando a {@code 1} a cada novo ano <b>por
 *       Organization</b>, ano corrente;</li>
 *   <li>Validar a invariante de cliente (exatamente um entre
 *       {@code customerId} e {@code companyId});</li>
 *   <li>Validar a existência do cliente e dos produtos referenciados;</li>
 *   <li>Garantir que a proposta tenha ao menos um item (serviço ou
 *       produto);</li>
 *   <li>Persistir o agregado (header + itens) e recalcular totais
 *       antes da resposta;</li>
 *   <li>Listar com filtros (status, intervalo de datas, cliente, código);</li>
 *   <li>Transições de status: {@code ABERTA → EM_ANDAMENTO → CONCLUIDA},
 *       preenchendo a data de entrega ao concluir.</li>
 * </ul>
 */
@Service
public class TechnicalProposalService {

    private final TechnicalProposalRepository repository;
    private final TechnicalProposalServiceItemRepository serviceItemRepository;
    private final TechnicalProposalProductItemRepository productItemRepository;
    private final TechnicalProposalConditionRepository conditionRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final CarrierRepository carrierRepository;
    private final OrganizationRepository organizationRepository;
    private final ReceivableService receivableService;
    private final ReceivableRepository receivableRepository;

    public TechnicalProposalService(TechnicalProposalRepository repository,
                                    TechnicalProposalServiceItemRepository serviceItemRepository,
                                    TechnicalProposalProductItemRepository productItemRepository,
                                    TechnicalProposalConditionRepository conditionRepository,
                                    CustomerRepository customerRepository,
                                    CompanyRepository companyRepository,
                                    ProductRepository productRepository,
                                    CarrierRepository carrierRepository,
                                    OrganizationRepository organizationRepository,
                                    ReceivableService receivableService,
                                    ReceivableRepository receivableRepository) {
        this.repository = repository;
        this.serviceItemRepository = serviceItemRepository;
        this.productItemRepository = productItemRepository;
        this.conditionRepository = conditionRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.carrierRepository = carrierRepository;
        this.organizationRepository = organizationRepository;
        this.receivableService = receivableService;
        this.receivableRepository = receivableRepository;
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Transactional
    public TechnicalProposalResponse create(TechnicalProposalCreateRequest request) {
        validateClientReference(request.customerId(), request.companyId(), true);
        validateCarrierReference(request.carrierId(), true);
        validateItemsPresence(request.serviceItems(), request.productItems());
        validateProductItems(request.productItems(), true);

        TechnicalProposal header = TechnicalProposalMapper.toEntity(request);
        applyNextCode(header);

        TechnicalProposal savedHeader = repository.save(header);

        List<TechnicalProposalServiceItem> serviceItems = persistServiceItems(
                request.serviceItems(), savedHeader.getId());
        List<TechnicalProposalProductItem> productItems = persistProductItems(
                request.productItems(), savedHeader.getId());
        List<TechnicalProposalCondition> conditions = persistConditions(
                request.conditions(), savedHeader.getId());

        savedHeader.recalculateTotals(serviceItems, productItems);

        ClientResolved client = resolveClient(savedHeader);
        CarrierResolved carrier = resolveCarrier(savedHeader);
        return TechnicalProposalMapper.toResponse(savedHeader,
                serviceItems, productItems, conditions, client.name(), client.code(),
                carrier.name());
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public TechnicalProposalResponse getById(Long id) {
        TechnicalProposal tp = repository.findById(id)
                .orElseThrow(() -> new TechnicalProposalNotFoundException(id));
        return toResponseWithItems(tp);
    }

    @Transactional(readOnly = true)
    public TechnicalProposalResponse getByCode(String code) {
        ParsedCode parsed = ParsedCode.parse(code);
        TechnicalProposal tp = repository
                .findByPrefixAndSequenceAndYear(parsed.prefix, parsed.sequence, parsed.year)
                .orElseThrow(() -> new TechnicalProposalNotFoundException(code));
        return toResponseWithItems(tp);
    }

    /**
     * Lista paginada com filtros opcionais. Todos os parâmetros são
     * opcionais; nulos significam "sem filtro".
     *
     * @param status      status da proposta (opcional)
     * @param startDate   data de início a partir de (opcional)
     * @param endDate     data de início até (opcional)
     * @param clientId  ID do cliente (PF ou PJ) (opcional)
     * @param codeLike    trecho do código (opcional)
     * @param pageable    paginação e ordenação
     */
    @Transactional(readOnly = true)
    public PagedResponse<TechnicalProposalSummaryResponse> search(TechnicalProposalStatus status,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   Long clientId,
                                                                   String codeLike,
                                                                   Pageable pageable) {
        Specification<TechnicalProposal> spec = (root, query, cb) -> {
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
            if (clientId != null) {
                jakarta.persistence.criteria.Predicate byCustomer =
                        cb.equal(root.get("customerId"), clientId);
                jakarta.persistence.criteria.Predicate byCompany =
                        cb.equal(root.get("companyId"), clientId);
                predicates.add(cb.or(byCustomer, byCompany));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<TechnicalProposal> page = repository.findAll(spec, pageable);

        // Filtro por trecho do código é aplicado em memória, pois o código
        // é composto (prefix + sequence + year) e formatado só em memória.
        // Quando não há filtro de código, map direto; quando há, filtramos
        // os resultados da página antes de montar a resposta.
        if (codeLike == null || codeLike.isBlank()) {
            Page<TechnicalProposalSummaryResponse> mapped = page.map(tp -> {
                loadItemsAndRecalc(tp);
                ClientResolved client = resolveClient(tp);
                return TechnicalProposalMapper.toSummary(tp, client.name(), client.code());
            });
            return PagedResponse.from(mapped);
        }

        String needle = codeLike.toLowerCase().trim();
        List<TechnicalProposalSummaryResponse> filtered = page.stream()
                .filter(tp -> tp.formattedCode().toLowerCase().contains(needle))
                .map(tp -> {
                    loadItemsAndRecalc(tp);
                    ClientResolved client = resolveClient(tp);
                    return TechnicalProposalMapper.toSummary(tp, client.name(), client.code());
                })
                .toList();
        // Sem recontar totalElements após filtro em memória: usamos o
        // total da página corrente como aproximação, já que o filtro de
        // código é opcional e raramente usado em listagens grandes.
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

    private void loadItemsAndRecalc(TechnicalProposal tp) {
        List<TechnicalProposalServiceItem> serviceItems = serviceItemRepository
                .findByTechnicalProposalIdOrderByCreatedAtAsc(tp.getId());
        List<TechnicalProposalProductItem> productItems = productItemRepository
                .findByTechnicalProposalIdOrderByCreatedAtAsc(tp.getId());
        tp.recalculateTotals(serviceItems, productItems);
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Transactional
    public TechnicalProposalResponse update(Long id, TechnicalProposalUpdateRequest request) {
        TechnicalProposal tp = repository.findById(id)
                .orElseThrow(() -> new TechnicalProposalNotFoundException(id));

        if (tp.getStatus() == TechnicalProposalStatus.CONCLUIDA) {
            throw new TechnicalProposalBusinessException(
                    "Proposta técnica CONCLUIDA não pode ser alterada.");
        }

        // Recalcula o cliente efetivo após o PATCH e valida
        Long effectiveCustomer = (request.customerId() != null)
                ? request.customerId() : tp.getCustomerId();
        Long effectiveCompany = (request.companyId() != null)
                ? request.companyId() : tp.getCompanyId();
        validateClientReference(effectiveCustomer, effectiveCompany, false);

        // Valida a carrier apenas quando explicitamente informada.
        if (request.carrierId() != null) {
            validateCarrierReference(request.carrierId(), false);
        }

        boolean servicesSent = request.serviceItems() != null;
        boolean productsSent = request.productItems() != null;
        boolean conditionsSent = request.conditions() != null;

        // Se alguma lista foi enviada, valida presença mínima entre as duas.
        if (servicesSent || productsSent) {
            List<TechnicalProposalServiceItemRequest> svc = servicesSent
                    ? request.serviceItems() : currentServiceRequests(tp);
            List<TechnicalProposalProductItemRequest> prod = productsSent
                    ? request.productItems() : currentProductRequests(tp);
            validateItemsPresence(svc, prod);
            if (productsSent) {
                validateProductItems(request.productItems(), false);
            }
        }

        // Substituição completa das listas, quando enviadas
        if (servicesSent) {
            serviceItemRepository.deleteByTechnicalProposalId(id);
            serviceItemRepository.flush();
        }
        if (productsSent) {
            productItemRepository.deleteByTechnicalProposalId(id);
            productItemRepository.flush();
        }
        if (conditionsSent) {
            conditionRepository.deleteByTechnicalProposalId(id);
            conditionRepository.flush();
        }

        TechnicalProposalMapper.applyUpdate(tp, request);
        TechnicalProposal saved = repository.save(tp);

        List<TechnicalProposalServiceItem> serviceItems = servicesSent
                ? persistServiceItems(request.serviceItems(), saved.getId())
                : serviceItemRepository.findByTechnicalProposalIdOrderByCreatedAtAsc(id);
        List<TechnicalProposalProductItem> productItems = productsSent
                ? persistProductItems(request.productItems(), saved.getId())
                : productItemRepository.findByTechnicalProposalIdOrderByCreatedAtAsc(id);
        List<TechnicalProposalCondition> conditions = conditionsSent
                ? persistConditions(request.conditions(), saved.getId())
                : conditionRepository.findByTechnicalProposalIdOrderBySortOrderAsc(id);

        saved.recalculateTotals(serviceItems, productItems);
        ClientResolved client = resolveClient(saved);
        CarrierResolved carrier = resolveCarrier(saved);
        return TechnicalProposalMapper.toResponse(saved,
                serviceItems, productItems, conditions, client.name(), client.code(),
                carrier.name());
    }

    // ---------------------------------------------------------------------
    // Transições de status
    // ---------------------------------------------------------------------

    /**
     * Inicia a execução: {@code ABERTA → EM_ANDAMENTO}. Gera
     * automaticamente a conta a receber vinculada à proposta.
     */
    @Transactional
    public TechnicalProposalResponse start(Long id) {
        TechnicalProposal tp = loadForStatusChange(id);
        if (tp.getStatus() != TechnicalProposalStatus.ABERTA) {
            throw new TechnicalProposalBusinessException(
                    "Apenas propostas ABERTA podem ser iniciadas. Status atual: " + tp.getStatus());
        }
        tp.setStatus(TechnicalProposalStatus.EM_ANDAMENTO);
        repository.save(tp);

        // Carrega itens e recalcula totais antes de gerar a conta a receber.
        TechnicalProposalResponse response = toResponseWithItems(tp);
        receivableService.generateFromTechnicalProposal(tp, tp.getTotal());
        return response;
    }

    /**
     * Conclui a execução: {@code EM_ANDAMENTO → CONCLUIDA}. Preenche
     * automaticamente a data de entrega com {@code LocalDate.now()}.
     */
    @Transactional
    public TechnicalProposalResponse complete(Long id) {
        TechnicalProposal tp = loadForStatusChange(id);
        if (tp.getStatus() != TechnicalProposalStatus.EM_ANDAMENTO) {
            throw new TechnicalProposalBusinessException(
                    "Apenas propostas EM_ANDAMENTO podem ser concluídas. Status atual: "
                            + tp.getStatus());
        }
        tp.setStatus(TechnicalProposalStatus.CONCLUIDA);
        tp.setDeliveryDate(LocalDate.now());
        repository.save(tp);

        return toResponseWithItems(tp);
    }

    /**
     * Reabre uma proposta CONCLUIDA para EM_ANDAMENTO, limpando a data
     * de entrega. Útil para corrigir conclusões indevidas.
     *
     * <p>Se existir uma conta a receber vinculada, ela será cancelada
     * apenas se ainda não tiver pagamentos registrados; caso contrário a
     * reabertura é bloqueada.</p>
     */
    @Transactional
    public TechnicalProposalResponse reopen(Long id) {
        TechnicalProposal tp = loadForStatusChange(id);
        if (tp.getStatus() != TechnicalProposalStatus.CONCLUIDA) {
            throw new TechnicalProposalBusinessException(
                    "Apenas propostas CONCLUIDA podem ser reabertas. Status atual: "
                            + tp.getStatus());
        }

        // Cancela (ou bloqueia) a conta a receber vinculada, se houver.
        receivableRepository.findActiveByTechnicalProposalId(tp.getId())
                .ifPresent(r -> receivableService.cancelIfNoPayments(
                        r.getId(), "a proposta técnica " + tp.formattedCode()));

        tp.setStatus(TechnicalProposalStatus.EM_ANDAMENTO);
        tp.setDeliveryDate(null);
        repository.save(tp);
        return toResponseWithItems(tp);
    }

    // ---------------------------------------------------------------------
    // Simulate (cálculo sem persistência)
    // ---------------------------------------------------------------------

    /**
     * Calcula os totais de uma proposta técnica sem persistir nada.
     * Usado pelo endpoint {@code POST /technical-proposals/simulate}
     * para que o frontend exiba um preview em tempo real — toda a lógica
     * de cálculo permanece no backend.
     *
     * <p>O request é permissivo: campos nulos são tratados como zero
     * pelo cálculo; listas vazias resultam em subtotal zero.</p>
     */
    @Transactional(readOnly = true)
    public TechnicalProposalSimulateResponse simulate(TechnicalProposalSimulateRequest request) {
        TechnicalProposal header = TechnicalProposalMapper.toEntity(request);
        List<TechnicalProposalServiceItem> serviceItems = (request.serviceItems() == null)
                ? List.of()
                : request.serviceItems().stream()
                        .map(req -> TechnicalProposalMapper.toServiceItemEntity(req, header.getId()))
                        .toList();
        List<TechnicalProposalProductItem> productItems = (request.productItems() == null)
                ? List.of()
                : request.productItems().stream()
                        .map(req -> TechnicalProposalMapper.toProductItemEntity(req, header.getId()))
                        .toList();

        header.recalculateTotals(serviceItems, productItems);
        return TechnicalProposalMapper.toSimulateResponse(header, serviceItems, productItems);
    }

    // ---------------------------------------------------------------------
    // Utilitários públicos
    // ---------------------------------------------------------------------

    /**
     * Retorna o código que seria atribuído à próxima proposta, sem
     * persistir nada. Útil para o frontend exibir o valor previsto no
     * formulário antes do envio.
     *
     * <p>O prefixo vem da {@code Organization} ativa e a sequência é
     * computada de forma independente por Organization/ano (multi-empresa).</p>
     */
    @Transactional(readOnly = true)
    public NextTechnicalProposalCodeResponse getNextCode() {
        Long orgId = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        String prefix = currentOrgProposalPrefix();
        long sequence = generateNextSequence(year, orgId);
        String code = prefix + "-" + formatSequence(sequence) + "-" + year;
        return new NextTechnicalProposalCodeResponse(prefix, sequence, year, code);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private TechnicalProposal loadForStatusChange(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TechnicalProposalNotFoundException(id));
    }

    private TechnicalProposalResponse toResponseWithItems(TechnicalProposal tp) {
        List<TechnicalProposalServiceItem> serviceItems = serviceItemRepository
                .findByTechnicalProposalIdOrderByCreatedAtAsc(tp.getId());
        List<TechnicalProposalProductItem> productItems = productItemRepository
                .findByTechnicalProposalIdOrderByCreatedAtAsc(tp.getId());
        List<TechnicalProposalCondition> conditions = conditionRepository
                .findByTechnicalProposalIdOrderBySortOrderAsc(tp.getId());
        tp.recalculateTotals(serviceItems, productItems);
        ClientResolved client = resolveClient(tp);
        CarrierResolved carrier = resolveCarrier(tp);
        return TechnicalProposalMapper.toResponse(tp,
                serviceItems, productItems, conditions, client.name(), client.code(),
                carrier.name());
    }

    /**
     * Aplica o próximo código disponível à proposta: prefixo da
     * {@code Organization} ativa, sequência independente por
     * Organization/ano e ano corrente.
     */
    private void applyNextCode(TechnicalProposal tp) {
        Long orgId = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        long sequence = generateNextSequence(year, orgId);
        tp.setPrefix(currentOrgProposalPrefix());
        tp.setSequence(sequence);
        tp.setYear(year);
    }

    private long generateNextSequence(int year, Long organizationId) {
        Long maxSequence = repository.findMaxSequenceByYearAndOrganizationId(year, organizationId);
        return (maxSequence == null) ? 1L : maxSequence + 1L;
    }

    /**
     * Resolve o prefixo de Proposta Técnica da {@code Organization}
     * ativa ({@link OrganizationContext}). Lança exceção de negócio
     * quando a Organization não tiver prefixo configurado (situação
     * que não deve ocorrer com o cadastro obrigatório + migration V25,
     * mas é tratada explicitamente para falhar de forma clara em vez
     * de gerar um código inválido como "-001-2026").
     */
    private String currentOrgProposalPrefix() {
        Long orgId = OrganizationContext.require();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new TechnicalProposalBusinessException(
                        "Organization ativa não encontrada: " + orgId));
        String prefix = org.getProposalPrefix();
        if (prefix == null || prefix.isBlank()) {
            throw new TechnicalProposalBusinessException(
                    "Organization ativa (" + org.getTradeName() + ") não possui "
                            + "proposalPrefix configurado. Atualize o cadastro da "
                            + "empresa antes de emitir propostas técnicas.");
        }
        return prefix;
    }

    private static String formatSequence(long sequence) {
        return String.format("%03d", sequence);
    }

    private List<TechnicalProposalServiceItem> persistServiceItems(
            List<TechnicalProposalServiceItemRequest> requests, Long technicalProposalId) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<TechnicalProposalServiceItem> items = new ArrayList<>(requests.size());
        for (TechnicalProposalServiceItemRequest req : requests) {
            items.add(serviceItemRepository.save(
                    TechnicalProposalMapper.toServiceItemEntity(req, technicalProposalId)));
        }
        return items;
    }

    private List<TechnicalProposalProductItem> persistProductItems(
            List<TechnicalProposalProductItemRequest> requests, Long technicalProposalId) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<TechnicalProposalProductItem> items = new ArrayList<>(requests.size());
        for (TechnicalProposalProductItemRequest req : requests) {
            items.add(productItemRepository.save(
                    TechnicalProposalMapper.toProductItemEntity(req, technicalProposalId)));
        }
        return items;
    }

    private List<TechnicalProposalCondition> persistConditions(
            List<TechnicalProposalConditionRequest> requests, Long technicalProposalId) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<TechnicalProposalCondition> items = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            items.add(conditionRepository.save(
                    TechnicalProposalMapper.toConditionEntity(requests.get(i), technicalProposalId, i)));
        }
        return items;
    }

    /**
     * Valida a invariante do cliente: exatamente um entre
     * {@code customerId} e {@code companyId} deve estar preenchido.
     * Quando {@code verifyExists} é verdadeiro (criação), também
     * verifica se o cliente existe no banco.
     */
    private void validateClientReference(Long customerId, Long companyId, boolean verifyExists) {
        if (customerId == null && companyId == null) {
            throw InvalidTechnicalProposalClientException.bothNull();
        }
        if (customerId != null && companyId != null) {
            throw InvalidTechnicalProposalClientException.bothSet();
        }
        if (!verifyExists) {
            return;
        }
        if (customerId != null && !customerRepository.existsById(customerId)) {
            throw new TechnicalProposalClientNotFoundException(customerId, "CUSTOMER");
        }
        if (companyId != null && !companyRepository.existsById(companyId)) {
            throw new TechnicalProposalClientNotFoundException(companyId, "COMPANY");
        }
    }

    /**
     * Valida que ao menos um item (serviço ou produto) está presente.
     */
    private void validateItemsPresence(List<TechnicalProposalServiceItemRequest> services,
                                       List<TechnicalProposalProductItemRequest> products) {
        boolean hasService = services != null && !services.isEmpty();
        boolean hasProduct = products != null && !products.isEmpty();
        if (!hasService && !hasProduct) {
            throw new TechnicalProposalBusinessException(
                    "A proposta técnica deve ter ao menos um serviço ou produto.");
        }
    }

    /**
     * Valida regras dos itens de produto: existência do produto.
     */
    private void validateProductItems(List<TechnicalProposalProductItemRequest> products,
                                      boolean verifyProductExists) {
        if (products == null || products.isEmpty()) {
            return;
        }
        for (int i = 0; i < products.size(); i++) {
            TechnicalProposalProductItemRequest it = products.get(i);
            if (verifyProductExists
                    && it.productId() != null
                    && !productRepository.existsById(it.productId())) {
                throw new TechnicalProposalBusinessException(
                        "Produto #" + (i + 1) + " não encontrado: " + it.productId());
            }
        }
    }

    /**
     * Resolve o nome e o código de exibição do cliente referenciado pela
     * proposta (PF: nome; PJ: nome fantasia se houver, senão razão
     * social). Retorna {@code null} em ambos os campos quando o registro
     * não existe mais, mantendo o ID como referência no DTO.
     */
    private ClientResolved resolveClient(TechnicalProposal tp) {
        if (tp.getCustomerId() != null) {
            return customerRepository.findById(tp.getCustomerId())
                    .map(c -> new ClientResolved(c.getName(), c.getCode()))
                    .orElse(ClientResolved.EMPTY);
        }
        if (tp.getCompanyId() != null) {
            return companyRepository.findById(tp.getCompanyId())
                    .map(c -> new ClientResolved(
                            c.getTradeName() != null && !c.getTradeName().isBlank()
                                    ? c.getTradeName()
                                    : c.getLegalName(),
                            c.getCode()))
                    .orElse(ClientResolved.EMPTY);
        }
        return ClientResolved.EMPTY;
    }

    /**
     * Valida a referência à transportadora (carrier): se não for nula,
     * verifica a existência no cadastro.
     */
    private void validateCarrierReference(Long carrierId, boolean verifyExists) {
        if (carrierId == null) {
            return;
        }
        if (verifyExists && !carrierRepository.existsById(carrierId)) {
            throw new TechnicalProposalBusinessException(
                    "Transportadora não encontrada: " + carrierId);
        }
    }

    /**
     * Resolve o nome da transportadora referenciada pela proposta
     * técnica. Retorna {@code null} quando a carrier não existe mais,
     * mantendo o ID como referência no DTO.
     */
    private CarrierResolved resolveCarrier(TechnicalProposal tp) {
        if (tp.getCarrierId() == null) {
            return CarrierResolved.EMPTY;
        }
        return carrierRepository.findById(tp.getCarrierId())
                .map(c -> new CarrierResolved(c.getName()))
                .orElse(CarrierResolved.EMPTY);
    }

    /**
     * Nome resolvido a partir da transportadora referenciada pela
     * proposta técnica.
     */
    private record CarrierResolved(String name) {
        static final CarrierResolved EMPTY = new CarrierResolved(null);
    }

    /**
     * Recupera os itens de serviço atuais como requests (usado para
     * validar a presença mínima no PATCH quando só uma das listas foi
     * enviada). Não altera nada.
     */
    private List<TechnicalProposalServiceItemRequest> currentServiceRequests(TechnicalProposal tp) {
        return serviceItemRepository
                .findByTechnicalProposalIdOrderByCreatedAtAsc(tp.getId())
                .stream()
                .map(item -> new TechnicalProposalServiceItemRequest(
                        item.getDescription(), item.getPrice(),
                        item.getCategory(), item.getServiceTemplateId()))
                .toList();
    }

    /**
     * Recupera os itens de produto atuais como requests (usado para
     * validar a presença mínima no PATCH quando só uma das listas foi
     * enviada). Não altera nada.
     */
    private List<TechnicalProposalProductItemRequest> currentProductRequests(TechnicalProposal tp) {
        return productItemRepository
                .findByTechnicalProposalIdOrderByCreatedAtAsc(tp.getId())
                .stream()
                .map(item -> new TechnicalProposalProductItemRequest(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()))
                .toList();
    }

    /**
     * Par (nome, código) resolvido a partir do cliente referenciado pela
     * proposta.
     */
    private record ClientResolved(String name, String code) {
        static final ClientResolved EMPTY = new ClientResolved(null, null);
    }

    /**
     * Parser do código formatado ({@code PL-001-2026}) nos campos
     * persistidos.
     */
    private record ParsedCode(String prefix, Long sequence, Integer year) {
        static ParsedCode parse(String code) {
            if (code == null || code.isBlank()) {
                throw new TechnicalProposalBusinessException("Código inválido: " + code);
            }
            String[] parts = code.trim().split("-");
            if (parts.length != 3) {
                throw new TechnicalProposalBusinessException(
                        "Código deve estar no formato PL-001-2026: " + code);
            }
            try {
                return new ParsedCode(
                        parts[0].toUpperCase(),
                        Long.parseLong(parts[1]),
                        Integer.parseInt(parts[2]));
            } catch (NumberFormatException ex) {
                throw new TechnicalProposalBusinessException(
                        "Código deve estar no formato PL-001-2026: " + code);
            }
        }
    }
}