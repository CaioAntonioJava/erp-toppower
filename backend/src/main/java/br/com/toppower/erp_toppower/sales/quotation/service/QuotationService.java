package br.com.toppower.erp_toppower.sales.quotation.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.carrier.repository.CarrierRepository;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.seller.repository.SellerRepository;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationCreateRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationItemRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSimulateRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSimulateResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationUpdateRequest;
import br.com.toppower.erp_toppower.sales.quotation.entity.Quotation;
import br.com.toppower.erp_toppower.sales.quotation.entity.QuotationItem;
import br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus;
import br.com.toppower.erp_toppower.sales.quotation.exception.InvalidQuotationClientException;
import br.com.toppower.erp_toppower.sales.quotation.exception.QuotationBusinessException;
import br.com.toppower.erp_toppower.sales.quotation.exception.QuotationClientNotFoundException;
import br.com.toppower.erp_toppower.sales.quotation.exception.QuotationNotFoundException;
import br.com.toppower.erp_toppower.sales.quotation.mapper.QuotationMapper;
import br.com.toppower.erp_toppower.sales.quotation.repository.QuotationItemRepository;
import br.com.toppower.erp_toppower.sales.quotation.repository.QuotationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negócio do ciclo de vida de uma proposta comercial.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>Gerar o número sequencial a partir de {@code QUO001500};</li>
 *   <li>Validar a invariante de cliente (exatamente um entre
 *       {@code customerId} e {@code companyId});</li>
 *   <li>Persistir o agregado (header + itens) garantindo que o
 *       {@code totalPrice} de cada item (líquido) seja calculado pelo
 *       mapper e os totais da proposta sejam recalculados antes da
 *       resposta;</li>
 *   <li>Listar com filtros (status, intervalo de datas, número, cliente);</li>
 *   <li>Cancelar (soft via status) e reativar.</li>
 * </ul>
 */
@Service
public class QuotationService {

    /** Valor inicial da sequência (primeira proposta emitida será {@code 1500}). */
    static final long INITIAL_NUMBER = 1500L;

    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final SellerRepository sellerRepository;
    private final CarrierRepository carrierRepository;

    public QuotationService(QuotationRepository quotationRepository,
                            QuotationItemRepository quotationItemRepository,
                            CustomerRepository customerRepository,
                            CompanyRepository companyRepository,
                            SellerRepository sellerRepository,
                            CarrierRepository carrierRepository) {
        this.quotationRepository = quotationRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.sellerRepository = sellerRepository;
        this.carrierRepository = carrierRepository;
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Transactional
    public QuotationResponse create(QuotationCreateRequest request) {
        validateClientReference(request.customerId(), request.companyId(), true);
        validateCarrierReference(request.carrierId(), true);
        validateItemsConsistency(request.items(), request.profitMargin(), null);

        Quotation header = QuotationMapper.toEntity(request);
        header.setNumber(generateNextNumber());

        // Persiste o header para obter o UUID que será referenciado pelos itens
        Quotation savedHeader = quotationRepository.save(header);

        // Persiste os itens, agora com o ID da proposta
        List<QuotationItem> items = new ArrayList<>(request.items().size());
        for (QuotationItemRequest itemReq : request.items()) {
            items.add(quotationItemRepository.save(
                    QuotationMapper.toItemEntity(itemReq, savedHeader.getId(),
                            savedHeader.getProfitMargin())));
        }

        // Recalcula e aplica os totais na entidade em memória (somente para a resposta)
        savedHeader.recalculateTotals(items);

        ClientResolved client = resolveClient(savedHeader);
        CarrierResolved carrier = resolveCarrier(savedHeader);
        return QuotationMapper.toResponse(savedHeader, items, resolveSellerName(savedHeader),
                client.name(), client.code(), carrier.name());
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public QuotationResponse getById(Long id) {
        Quotation q = quotationRepository.findById(id)
                .orElseThrow(() -> new QuotationNotFoundException(id));
        List<QuotationItem> items = quotationItemRepository
                .findByQuotationIdOrderByCreatedAtAsc(id);
        q.recalculateTotals(items);
        ClientResolved client = resolveClient(q);
        CarrierResolved carrier = resolveCarrier(q);
        return QuotationMapper.toResponse(q, items, resolveSellerName(q),
                client.name(), client.code(), carrier.name());
    }

    @Transactional(readOnly = true)
    public QuotationResponse getByNumber(Long number) {
        // A numeração é independente por Organization: o mesmo número pode
        // existir em empresas diferentes. Restringe a busca à Organization
        // ativa para não devolver a proposta de outra empresa.
        Long orgId = OrganizationContext.require();
        Quotation q = quotationRepository.findByNumberAndOrganizationId(number, orgId)
                .orElseThrow(() -> new QuotationNotFoundException(number));
        List<QuotationItem> items = quotationItemRepository
                .findByQuotationIdOrderByCreatedAtAsc(q.getId());
        q.recalculateTotals(items);
        ClientResolved client = resolveClient(q);
        CarrierResolved carrier = resolveCarrier(q);
        return QuotationMapper.toResponse(q, items, resolveSellerName(q),
                client.name(), client.code(), carrier.name());
    }

    /**
     * Lista paginada com filtros opcionais. Todos os parâmetros são
     * opcionais; nulos significam "sem filtro".
     *
     * @param status      status da proposta (opcional)
     * @param startDate   data de emissão a partir de (opcional)
     * @param endDate     data de emissão até (opcional)
     * @param clientId   ID do cliente (PF ou PJ) (opcional)
     * @param sellerId   ID do vendedor (opcional)
     * @param numberLike  trecho do número (opcional)
     * @param pageable    paginação e ordenação
     */
    @Transactional(readOnly = true)
    public PagedResponse<QuotationSummaryResponse> search(QuotationStatus status,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          Long clientId,
                                                          Long sellerId,
                                                          String numberLike,
                                                          Pageable pageable) {
        Specification<Quotation> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issueDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issueDate"), endDate));
            }
            if (clientId != null) {
                jakarta.persistence.criteria.Predicate byCustomer =
                        cb.equal(root.get("customerId"), clientId);
                jakarta.persistence.criteria.Predicate byCompany =
                        cb.equal(root.get("companyId"), clientId);
                predicates.add(cb.or(byCustomer, byCompany));
            }
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("sellerId"), sellerId));
            }
            if (numberLike != null && !numberLike.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("number")),
                        "%" + numberLike.toLowerCase().trim() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Quotation> page = quotationRepository.findAll(spec, pageable);

        // Carrega itens e calcula totais por proposta
        Page<QuotationSummaryResponse> mapped = page.map(q -> {
            List<QuotationItem> items = quotationItemRepository
                    .findByQuotationIdOrderByCreatedAtAsc(q.getId());
            q.recalculateTotals(items);
            ClientResolved client = resolveClient(q);
            String sellerName = resolveSellerName(q);
            return QuotationMapper.toSummary(q, client.name(), client.code(), sellerName);
        });
        return PagedResponse.from(mapped);
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Transactional
    public QuotationResponse update(Long id, QuotationUpdateRequest request) {
        Quotation q = quotationRepository.findById(id)
                .orElseThrow(() -> new QuotationNotFoundException(id));

        if (q.getStatus() == QuotationStatus.CONVERTIDA) {
            throw new QuotationBusinessException(
                    "Proposta CONVERTIDA em pedido não pode ser alterada.");
        }

        // Recalcula o cliente efetivo após o PATCH e valida
        Long effectiveCustomer = (request.customerId() != null)
                ? request.customerId() : q.getCustomerId();
        Long effectiveCompany = (request.companyId() != null)
                ? request.companyId() : q.getCompanyId();
        validateClientReference(effectiveCustomer, effectiveCompany, false);

        // Valida a carrier apenas quando o campo foi explicitamente
        // informado (não-nulo). null no PATCH = remover a transportadora
        // (QuotationMapper.applyUpdate grava null nesse caso).
        if (request.carrierId() != null) {
            validateCarrierReference(request.carrierId(), false);
        }

        // Se a lista de itens foi enviada, valida e substitui por completo
        if (request.items() != null) {
            BigDecimal effectiveHeaderMargin = (request.profitMargin() != null)
                    ? request.profitMargin() : q.getProfitMargin();
            validateItemsConsistency(request.items(), effectiveHeaderMargin, null);
            quotationItemRepository.deleteByQuotationId(id);
            quotationItemRepository.flush();
        }

        QuotationMapper.applyUpdate(q, request);
        Quotation saved = quotationRepository.save(q);

        // Recria os itens quando aplicável
        List<QuotationItem> items;
        if (request.items() != null) {
            items = new ArrayList<>(request.items().size());
            for (QuotationItemRequest itemReq : request.items()) {
                items.add(quotationItemRepository.save(
                        QuotationMapper.toItemEntity(itemReq, saved.getId(),
                                saved.getProfitMargin())));
            }
        } else {
            items = quotationItemRepository.findByQuotationIdOrderByCreatedAtAsc(id);
        }

        saved.recalculateTotals(items);
        ClientResolved client = resolveClient(saved);
        CarrierResolved carrier = resolveCarrier(saved);
        return QuotationMapper.toResponse(saved, items, resolveSellerName(saved),
                client.name(), client.code(), carrier.name());
    }

    // ---------------------------------------------------------------------
    // Cancel (soft via status)
    // ---------------------------------------------------------------------

    @Transactional
    public QuotationResponse cancel(Long id) {
        Quotation q = quotationRepository.findById(id)
                .orElseThrow(() -> new QuotationNotFoundException(id));
        if (q.getStatus() == QuotationStatus.CANCELADA) {
            throw new QuotationBusinessException("Proposta já está cancelada.");
        }
        if (q.getStatus() == QuotationStatus.CONVERTIDA) {
            throw new QuotationBusinessException(
                    "Proposta CONVERTIDA em pedido não pode ser cancelada.");
        }
        q.setStatus(QuotationStatus.CANCELADA);
        Quotation saved = quotationRepository.save(q);

        List<QuotationItem> items = quotationItemRepository
                .findByQuotationIdOrderByCreatedAtAsc(id);
        saved.recalculateTotals(items);
        ClientResolved client = resolveClient(saved);
        CarrierResolved carrier = resolveCarrier(saved);
        return QuotationMapper.toResponse(saved, items, resolveSellerName(saved),
                client.name(), client.code(), carrier.name());
    }

    // ---------------------------------------------------------------------
    // Simulate (cálculo sem persistência)
    // ---------------------------------------------------------------------

    /**
     * Calcula os totais de uma proposta comercial sem persistir nada.
     * Usado pelo endpoint {@code POST /quotations/simulate} para que o
     * frontend exiba um preview em tempo real — toda a lógica de cálculo
     * permanece no backend.
     *
     * <p>Diferente do {@link #create(QuotationCreateRequest)}, este método
     * <b>não</b> exige cliente/vendedor/itens preenchidos: o request é
     * permissivo, pois o preview pode ser disparado com o formulário em
     * estado intermediário. Itens nulos/vazios resultam em totais zero;
     * campos numéricos nulos são tratados como zero pelo cálculo.</p>
     */
    @Transactional(readOnly = true)
    public QuotationSimulateResponse simulate(QuotationSimulateRequest request) {
        Quotation header = QuotationMapper.toEntity(request);
        List<QuotationItem> items = (request.items() == null)
                ? List.of()
                : request.items().stream()
                        .map(itemReq -> QuotationMapper.toItemEntity(itemReq, header.getId(),
                                header.getProfitMargin()))
                        .toList();

        header.recalculateTotals(items);
        return QuotationMapper.toSimulateResponse(header, items);
    }

    // ---------------------------------------------------------------------
    // Utilitários públicos
    // ---------------------------------------------------------------------

    /**
     * Retorna o próximo número que seria atribuído a uma nova proposta,
     * sem persistir nada. Útil para o frontend exibir o valor previsto
     * no formulário antes do envio.
     */
    @Transactional(readOnly = true)
    public Long getNextNumber() {
        // A pré-visualização do próximo número é independente por
        // Organization: cada empresa tem sua própria sequência.
        return generateNextNumber();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Gera o próximo número sequencial de proposta para a Organization
     * ativa (a partir de {@code 1500} na primeira proposta da empresa).
     * A sequência é independente por Organization — cada empresa reinicia
     * em 1500 e incrementa de +1 em +1, sem compartilhar a contagem com
     * as demais.
     */
    private Long generateNextNumber() {
        Long orgId = OrganizationContext.require();
        Long maxNumber = quotationRepository.findMaxNumberByOrganizationId(orgId);
        if (maxNumber == null) {
            // Nenhuma proposta cadastrada para esta Organization — inicia em 1500
            return INITIAL_NUMBER;
        }
        return maxNumber + 1L;
    }

    /**
     * Valida a invariante do cliente: exatamente um entre
     * {@code customerId} e {@code companyId} deve estar preenchido.
     * Quando {@code verifyExists} é verdadeiro (criação), também
     * verifica se o cliente existe no banco.
     */
    private void validateClientReference(Long customerId, Long companyId, boolean verifyExists) {
        if (customerId == null && companyId == null) {
            throw InvalidQuotationClientException.bothNull();
        }
        if (customerId != null && companyId != null) {
            throw InvalidQuotationClientException.bothSet();
        }
        if (!verifyExists) {
            return;
        }
        if (customerId != null && !customerRepository.existsById(customerId)) {
            throw new QuotationClientNotFoundException(customerId, "CUSTOMER");
        }
        if (companyId != null && !companyRepository.existsById(companyId)) {
            throw new QuotationClientNotFoundException(companyId, "COMPANY");
        }
    }

    /**
     * Valida regras de negócio dos itens: lista não vazia e consistência
     * da margem (cabeçalho ou por item).
     *
     * <p>A margem de lucro é obrigatória em algum nível: ou o cabeçalho
     * informa {@code profitMargin}, ou ao menos um item informa sua
     * própria margem. Itens sem margem própria herdam a do cabeçalho;
     * itens com margem própria a sobrescrevem.</p>
     *
     * @param items             itens a validar
     * @param headerProfitMargin margem do cabeçalho (pode ser nula quando
     *                            todos os itens têm margem própria)
     * @param currentQuotationId ID da proposta (reservado para validações
     *                           futuras, ex.: unicidade de produto)
     */
    private void validateItemsConsistency(List<QuotationItemRequest> items,
                                          BigDecimal headerProfitMargin,
                                          Long currentQuotationId) {
        if (items == null || items.isEmpty()) {
            throw new QuotationBusinessException("A proposta deve ter ao menos um item.");
        }
        boolean algumItemComMargem = items.stream()
                .anyMatch(it -> it.profitMargin() != null && it.profitMargin().signum() != 0);
        if (headerProfitMargin == null && !algumItemComMargem) {
            throw new QuotationBusinessException(
                    "Informe a margem de lucro do cabeçalho ou a margem de cada item.");
        }
    }

    /**
     * Resolve o nome e o código de exibição do cliente referenciado pela
     * proposta (PF: nome; PJ: nome fantasia se houver, senão razão social).
     * Retorna {@code null} em ambos os campos quando o registro não existe
     * mais (inativado/removido), mantendo o ID como referência no DTO —
     * mesmo tratamento dado a {@link #resolveSellerName(Quotation)}.
     */
    private ClientResolved resolveClient(Quotation q) {
        if (q.getCustomerId() != null) {
            return customerRepository.findById(q.getCustomerId())
                    .map(c -> new ClientResolved(c.getName(), c.getCode()))
                    .orElse(ClientResolved.EMPTY);
        }
        if (q.getCompanyId() != null) {
            return companyRepository.findById(q.getCompanyId())
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
     * Par (nome, código) resolvido a partir do cliente referenciado pela
     * proposta. Usado para popular {@code clientName} e {@code clientCode}
     * no {@link QuotationResponse} e {@link QuotationSummaryResponse}.
     */
    private record ClientResolved(String name, String code) {
        static final ClientResolved EMPTY = new ClientResolved(null, null);
    }

    /**
     * Valida a referência à transportadora (carrier): se não for nula,
     * verifica a existência no cadastro. Quando {@code verifyExists} é
     * verdadeiro (criação), também exige que o registro exista — usado
     * para dar mensagem amigável antes de chegar ao controller.
     */
    private void validateCarrierReference(Long carrierId, boolean verifyExists) {
        if (carrierId == null) {
            return;
        }
        if (verifyExists && !carrierRepository.existsById(carrierId)) {
            throw new QuotationBusinessException(
                    "Transportadora não encontrada: " + carrierId);
        }
    }

    /**
     * Resolve o nome da transportadora referenciada pela proposta.
     * Retorna {@code null} quando a carrier não existe mais
     * (inativada/removida), mantendo o ID como referência no DTO —
     * mesmo tratamento dado a {@link #resolveSellerName}.
     */
    private CarrierResolved resolveCarrier(Quotation q) {
        if (q.getCarrierId() == null) {
            return CarrierResolved.EMPTY;
        }
        return carrierRepository.findById(q.getCarrierId())
                .map(c -> new CarrierResolved(c.getName()))
                .orElse(CarrierResolved.EMPTY);
    }

    /**
     * Nome resolvido a partir da transportadora referenciada pela
     * proposta.
     */
    private record CarrierResolved(String name) {
        static final CarrierResolved EMPTY = new CarrierResolved(null);
    }

    /**
     * Resolve o nome do vendedor referenciado pela proposta. Retorna
     * {@code null} quando o vendedor não existe mais (inativado/removido),
     * mantendo o ID como referência no DTO.
     */
    private String resolveSellerName(Quotation q) {
        if (q.getSellerId() == null) {
            return null;
        }
        return sellerRepository.findById(q.getSellerId())
                .map(s -> s.getName())
                .orElse(null);
    }
}
