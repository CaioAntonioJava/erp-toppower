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
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalCreateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalObjectiveRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalProductItemRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalServiceItemRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSimulateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSimulateResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSummaryResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalUpdateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposal;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalObjective;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalProductItem;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalServiceItem;
import br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus;
import br.com.toppower.erp_toppower.sales.technicalproposal.exception.InvalidTechnicalProposalClientException;
import br.com.toppower.erp_toppower.sales.technicalproposal.exception.TechnicalProposalBusinessException;
import br.com.toppower.erp_toppower.sales.technicalproposal.exception.TechnicalProposalClientNotFoundException;
import br.com.toppower.erp_toppower.sales.technicalproposal.exception.TechnicalProposalNotFoundException;
import br.com.toppower.erp_toppower.sales.technicalproposal.mapper.TechnicalProposalMapper;
import br.com.toppower.erp_toppower.sales.technicalproposal.repository.TechnicalProposalObjectiveRepository;
import br.com.toppower.erp_toppower.sales.technicalproposal.repository.TechnicalProposalProductItemRepository;
import br.com.toppower.erp_toppower.sales.technicalproposal.repository.TechnicalProposalRepository;
import br.com.toppower.erp_toppower.sales.technicalproposal.repository.TechnicalProposalServiceItemRepository;
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
 *       {@code customerUuid} e {@code companyUuid});</li>
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
    private final TechnicalProposalObjectiveRepository objectiveRepository;
    private final TechnicalProposalServiceItemRepository serviceItemRepository;
    private final TechnicalProposalProductItemRepository productItemRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final CarrierRepository carrierRepository;
    private final OrganizationRepository organizationRepository;

    public TechnicalProposalService(TechnicalProposalRepository repository,
                                    TechnicalProposalObjectiveRepository objectiveRepository,
                                    TechnicalProposalServiceItemRepository serviceItemRepository,
                                    TechnicalProposalProductItemRepository productItemRepository,
                                    CustomerRepository customerRepository,
                                    CompanyRepository companyRepository,
                                    ProductRepository productRepository,
                                    CarrierRepository carrierRepository,
                                    OrganizationRepository organizationRepository) {
        this.repository = repository;
        this.objectiveRepository = objectiveRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.productItemRepository = productItemRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.carrierRepository = carrierRepository;
        this.organizationRepository = organizationRepository;
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Transactional
    public TechnicalProposalResponse create(TechnicalProposalCreateRequest request) {
        validateClientReference(request.customerUuid(), request.companyUuid(), true);
        validateCarrierReference(request.carrierUuid(), true);
        validateObjectives(request.objectives());
        validateItemsPresence(request.serviceItems(), request.productItems());
        validateProductItems(request.productItems(), true);

        TechnicalProposal header = TechnicalProposalMapper.toEntity(request);
        applyNextCode(header);

        TechnicalProposal savedHeader = repository.save(header);

        List<TechnicalProposalObjective> objectives = persistObjectives(
                request.objectives(), savedHeader.getUuid());
        List<TechnicalProposalServiceItem> serviceItems = persistServiceItems(
                request.serviceItems(), savedHeader.getUuid());
        List<TechnicalProposalProductItem> productItems = persistProductItems(
                request.productItems(), savedHeader.getUuid());

        savedHeader.recalculateTotals(serviceItems, productItems);

        ClientResolved client = resolveClient(savedHeader);
        CarrierResolved carrier = resolveCarrier(savedHeader);
        return TechnicalProposalMapper.toResponse(savedHeader, objectives,
                serviceItems, productItems, client.name(), client.code(),
                carrier.name());
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public TechnicalProposalResponse getById(UUID id) {
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
     * @param clientUuid  UUID do cliente (PF ou PJ) (opcional)
     * @param codeLike    trecho do código (opcional)
     * @param pageable    paginação e ordenação
     */
    @Transactional(readOnly = true)
    public PagedResponse<TechnicalProposalSummaryResponse> search(TechnicalProposalStatus status,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   UUID clientUuid,
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
            if (clientUuid != null) {
                jakarta.persistence.criteria.Predicate byCustomer =
                        cb.equal(root.get("customerUuid"), clientUuid);
                jakarta.persistence.criteria.Predicate byCompany =
                        cb.equal(root.get("companyUuid"), clientUuid);
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
                List<TechnicalProposalObjective> objs = loadObjectives(tp);
                ClientResolved client = resolveClient(tp);
                return TechnicalProposalMapper.toSummary(tp, objs, client.name(), client.code());
            });
            return PagedResponse.from(mapped);
        }

        String needle = codeLike.toLowerCase().trim();
        List<TechnicalProposalSummaryResponse> filtered = page.stream()
                .filter(tp -> tp.formattedCode().toLowerCase().contains(needle))
                .map(tp -> {
                    loadItemsAndRecalc(tp);
                    List<TechnicalProposalObjective> objs = loadObjectives(tp);
                    ClientResolved client = resolveClient(tp);
                    return TechnicalProposalMapper.toSummary(tp, objs, client.name(), client.code());
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
                .findByTechnicalProposalUuidOrderByCreatedAtAsc(tp.getUuid());
        List<TechnicalProposalProductItem> productItems = productItemRepository
                .findByTechnicalProposalUuidOrderByCreatedAtAsc(tp.getUuid());
        tp.recalculateTotals(serviceItems, productItems);
    }

    private List<TechnicalProposalObjective> loadObjectives(TechnicalProposal tp) {
        return objectiveRepository
                .findByTechnicalProposalUuidOrderByCreatedAtAsc(tp.getUuid());
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Transactional
    public TechnicalProposalResponse update(UUID id, TechnicalProposalUpdateRequest request) {
        TechnicalProposal tp = repository.findById(id)
                .orElseThrow(() -> new TechnicalProposalNotFoundException(id));

        if (tp.getStatus() == TechnicalProposalStatus.CONCLUIDA) {
            throw new TechnicalProposalBusinessException(
                    "Proposta técnica CONCLUIDA não pode ser alterada.");
        }

        // Recalcula o cliente efetivo após o PATCH e valida
        UUID effectiveCustomer = (request.customerUuid() != null)
                ? request.customerUuid() : tp.getCustomerUuid();
        UUID effectiveCompany = (request.companyUuid() != null)
                ? request.companyUuid() : tp.getCompanyUuid();
        validateClientReference(effectiveCustomer, effectiveCompany, false);

        // Valida a carrier apenas quando explicitamente informada.
        if (request.carrierUuid() != null) {
            validateCarrierReference(request.carrierUuid(), false);
        }

        boolean objectivesSent = request.objectives() != null;
        boolean servicesSent = request.serviceItems() != null;
        boolean productsSent = request.productItems() != null;

        // Valida a nova lista de objetivos, quando enviada.
        if (objectivesSent) {
            validateObjectives(request.objectives());
        }

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
        if (objectivesSent) {
            objectiveRepository.deleteByTechnicalProposalUuid(id);
            objectiveRepository.flush();
        }
        if (servicesSent) {
            serviceItemRepository.deleteByTechnicalProposalUuid(id);
            serviceItemRepository.flush();
        }
        if (productsSent) {
            productItemRepository.deleteByTechnicalProposalUuid(id);
            productItemRepository.flush();
        }

        TechnicalProposalMapper.applyUpdate(tp, request);
        TechnicalProposal saved = repository.save(tp);

        List<TechnicalProposalObjective> objectives = objectivesSent
                ? persistObjectives(request.objectives(), saved.getUuid())
                : objectiveRepository.findByTechnicalProposalUuidOrderByCreatedAtAsc(id);
        List<TechnicalProposalServiceItem> serviceItems = servicesSent
                ? persistServiceItems(request.serviceItems(), saved.getUuid())
                : serviceItemRepository.findByTechnicalProposalUuidOrderByCreatedAtAsc(id);
        List<TechnicalProposalProductItem> productItems = productsSent
                ? persistProductItems(request.productItems(), saved.getUuid())
                : productItemRepository.findByTechnicalProposalUuidOrderByCreatedAtAsc(id);

        saved.recalculateTotals(serviceItems, productItems);
        ClientResolved client = resolveClient(saved);
        CarrierResolved carrier = resolveCarrier(saved);
        return TechnicalProposalMapper.toResponse(saved, objectives,
                serviceItems, productItems, client.name(), client.code(),
                carrier.name());
    }

    // ---------------------------------------------------------------------
    // Transições de status
    // ---------------------------------------------------------------------

    /**
     * Inicia a execução: {@code ABERTA → EM_ANDAMENTO}.
     */
    @Transactional
    public TechnicalProposalResponse start(UUID id) {
        TechnicalProposal tp = loadForStatusChange(id);
        if (tp.getStatus() != TechnicalProposalStatus.ABERTA) {
            throw new TechnicalProposalBusinessException(
                    "Apenas propostas ABERTA podem ser iniciadas. Status atual: " + tp.getStatus());
        }
        tp.setStatus(TechnicalProposalStatus.EM_ANDAMENTO);
        repository.save(tp);
        return toResponseWithItems(tp);
    }

    /**
     * Conclui a execução: {@code EM_ANDAMENTO → CONCLUIDA}. Preenche
     * automaticamente a data de entrega com {@code LocalDate.now()}.
     */
    @Transactional
    public TechnicalProposalResponse complete(UUID id) {
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
     */
    @Transactional
    public TechnicalProposalResponse reopen(UUID id) {
        TechnicalProposal tp = loadForStatusChange(id);
        if (tp.getStatus() != TechnicalProposalStatus.CONCLUIDA) {
            throw new TechnicalProposalBusinessException(
                    "Apenas propostas CONCLUIDA podem ser reabertas. Status atual: "
                            + tp.getStatus());
        }
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
                        .map(req -> TechnicalProposalMapper.toServiceItemEntity(req, header.getUuid()))
                        .toList();
        List<TechnicalProposalProductItem> productItems = (request.productItems() == null)
                ? List.of()
                : request.productItems().stream()
                        .map(req -> TechnicalProposalMapper.toProductItemEntity(req, header.getUuid()))
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
        UUID orgUuid = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        String prefix = currentOrgProposalPrefix();
        long sequence = generateNextSequence(year, orgUuid);
        String code = prefix + "-" + formatSequence(sequence) + "-" + year;
        return new NextTechnicalProposalCodeResponse(prefix, sequence, year, code);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private TechnicalProposal loadForStatusChange(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new TechnicalProposalNotFoundException(id));
    }

    private TechnicalProposalResponse toResponseWithItems(TechnicalProposal tp) {
        List<TechnicalProposalObjective> objectives = objectiveRepository
                .findByTechnicalProposalUuidOrderByCreatedAtAsc(tp.getUuid());
        List<TechnicalProposalServiceItem> serviceItems = serviceItemRepository
                .findByTechnicalProposalUuidOrderByCreatedAtAsc(tp.getUuid());
        List<TechnicalProposalProductItem> productItems = productItemRepository
                .findByTechnicalProposalUuidOrderByCreatedAtAsc(tp.getUuid());
        tp.recalculateTotals(serviceItems, productItems);
        ClientResolved client = resolveClient(tp);
        CarrierResolved carrier = resolveCarrier(tp);
        return TechnicalProposalMapper.toResponse(tp, objectives,
                serviceItems, productItems, client.name(), client.code(),
                carrier.name());
    }

    /**
     * Aplica o próximo código disponível à proposta: prefixo da
     * {@code Organization} ativa, sequência independente por
     * Organization/ano e ano corrente.
     */
    private void applyNextCode(TechnicalProposal tp) {
        UUID orgUuid = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        long sequence = generateNextSequence(year, orgUuid);
        tp.setPrefix(currentOrgProposalPrefix());
        tp.setSequence(sequence);
        tp.setYear(year);
    }

    private long generateNextSequence(int year, UUID organizationUuid) {
        Long maxSequence = repository.findMaxSequenceByYearAndOrganizationUuid(year, organizationUuid);
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
        UUID orgUuid = OrganizationContext.require();
        Organization org = organizationRepository.findById(orgUuid)
                .orElseThrow(() -> new TechnicalProposalBusinessException(
                        "Organization ativa não encontrada: " + orgUuid));
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

    private List<TechnicalProposalObjective> persistObjectives(
            List<TechnicalProposalObjectiveRequest> requests, UUID technicalProposalUuid) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<TechnicalProposalObjective> items = new ArrayList<>(requests.size());
        for (TechnicalProposalObjectiveRequest req : requests) {
            items.add(objectiveRepository.save(
                    TechnicalProposalMapper.toObjectiveEntity(req, technicalProposalUuid)));
        }
        return items;
    }

    private List<TechnicalProposalServiceItem> persistServiceItems(
            List<TechnicalProposalServiceItemRequest> requests, UUID technicalProposalUuid) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<TechnicalProposalServiceItem> items = new ArrayList<>(requests.size());
        for (TechnicalProposalServiceItemRequest req : requests) {
            items.add(serviceItemRepository.save(
                    TechnicalProposalMapper.toServiceItemEntity(req, technicalProposalUuid)));
        }
        return items;
    }

    private List<TechnicalProposalProductItem> persistProductItems(
            List<TechnicalProposalProductItemRequest> requests, UUID technicalProposalUuid) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<TechnicalProposalProductItem> items = new ArrayList<>(requests.size());
        for (TechnicalProposalProductItemRequest req : requests) {
            items.add(productItemRepository.save(
                    TechnicalProposalMapper.toProductItemEntity(req, technicalProposalUuid)));
        }
        return items;
    }

    /**
     * Valida a lista de objetivos: deve ser não-nula, não-vazia e cada
     * descrição deve respeitar o tamanho máximo (validação de bean
     * já feita pelo {@code @Valid}, mas checamos aqui para lançar
     * exceção de negócio com mensagem amigável antes de chegar ao
     * controller).
     */
    private void validateObjectives(List<TechnicalProposalObjectiveRequest> objectives) {
        if (objectives == null || objectives.isEmpty()) {
            throw new TechnicalProposalBusinessException(
                    "A proposta técnica deve ter ao menos um objetivo.");
        }
        for (int i = 0; i < objectives.size(); i++) {
            TechnicalProposalObjectiveRequest o = objectives.get(i);
            if (o.description() == null || o.description().isBlank()) {
                throw new TechnicalProposalBusinessException(
                        "Objetivo #" + (i + 1) + ": descrição é obrigatória.");
            }
            if (o.description().length() > 500) {
                throw new TechnicalProposalBusinessException(
                        "Objetivo #" + (i + 1)
                                + ": descrição deve ter no máximo 500 caracteres.");
            }
        }
    }

    /**
     * Valida a invariante do cliente: exatamente um entre
     * {@code customerUuid} e {@code companyUuid} deve estar preenchido.
     * Quando {@code verifyExists} é verdadeiro (criação), também
     * verifica se o cliente existe no banco.
     */
    private void validateClientReference(UUID customerUuid, UUID companyUuid, boolean verifyExists) {
        if (customerUuid == null && companyUuid == null) {
            throw InvalidTechnicalProposalClientException.bothNull();
        }
        if (customerUuid != null && companyUuid != null) {
            throw InvalidTechnicalProposalClientException.bothSet();
        }
        if (!verifyExists) {
            return;
        }
        if (customerUuid != null && !customerRepository.existsById(customerUuid)) {
            throw new TechnicalProposalClientNotFoundException(customerUuid, "CUSTOMER");
        }
        if (companyUuid != null && !companyRepository.existsById(companyUuid)) {
            throw new TechnicalProposalClientNotFoundException(companyUuid, "COMPANY");
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
     * Valida regras dos itens de produto: existência do produto e
     * consistência entre {@code discount} e {@code discountType}.
     */
    private void validateProductItems(List<TechnicalProposalProductItemRequest> products,
                                      boolean verifyProductExists) {
        if (products == null || products.isEmpty()) {
            return;
        }
        for (int i = 0; i < products.size(); i++) {
            TechnicalProposalProductItemRequest it = products.get(i);
            if (it.discount() != null && it.discountType() == null) {
                throw new TechnicalProposalBusinessException(
                        "Produto #" + (i + 1)
                                + ": discountType é obrigatório quando discount é informado.");
            }
            if (it.discountType() != null
                    && (it.discount() == null || it.discount().signum() == 0)) {
                throw new TechnicalProposalBusinessException(
                        "Produto #" + (i + 1)
                                + ": discount é obrigatório quando discountType é informado.");
            }
            if (verifyProductExists
                    && it.productUuid() != null
                    && !productRepository.existsById(it.productUuid())) {
                throw new TechnicalProposalBusinessException(
                        "Produto #" + (i + 1) + " não encontrado: " + it.productUuid());
            }
        }
    }

    /**
     * Resolve o nome e o código de exibição do cliente referenciado pela
     * proposta (PF: nome; PJ: nome fantasia se houver, senão razão
     * social). Retorna {@code null} em ambos os campos quando o registro
     * não existe mais, mantendo o UUID como referência no DTO.
     */
    private ClientResolved resolveClient(TechnicalProposal tp) {
        if (tp.getCustomerUuid() != null) {
            return customerRepository.findById(tp.getCustomerUuid())
                    .map(c -> new ClientResolved(c.getName(), c.getCode()))
                    .orElse(ClientResolved.EMPTY);
        }
        if (tp.getCompanyUuid() != null) {
            return companyRepository.findById(tp.getCompanyUuid())
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
    private void validateCarrierReference(UUID carrierUuid, boolean verifyExists) {
        if (carrierUuid == null) {
            return;
        }
        if (verifyExists && !carrierRepository.existsById(carrierUuid)) {
            throw new TechnicalProposalBusinessException(
                    "Transportadora não encontrada: " + carrierUuid);
        }
    }

    /**
     * Resolve o nome da transportadora referenciada pela proposta
     * técnica. Retorna {@code null} quando a carrier não existe mais,
     * mantendo o UUID como referência no DTO.
     */
    private CarrierResolved resolveCarrier(TechnicalProposal tp) {
        if (tp.getCarrierUuid() == null) {
            return CarrierResolved.EMPTY;
        }
        return carrierRepository.findById(tp.getCarrierUuid())
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
                .findByTechnicalProposalUuidOrderByCreatedAtAsc(tp.getUuid())
                .stream()
                .map(item -> new TechnicalProposalServiceItemRequest(
                        item.getDescription(), item.getPrice()))
                .toList();
    }

    /**
     * Recupera os itens de produto atuais como requests (usado para
     * validar a presença mínima no PATCH quando só uma das listas foi
     * enviada). Não altera nada.
     */
    private List<TechnicalProposalProductItemRequest> currentProductRequests(TechnicalProposal tp) {
        return productItemRepository
                .findByTechnicalProposalUuidOrderByCreatedAtAsc(tp.getUuid())
                .stream()
                .map(item -> new TechnicalProposalProductItemRequest(
                        item.getProductUuid(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getDiscountType(),
                        item.getDiscount()))
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