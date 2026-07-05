package br.com.toppower.erp_toppower.sales.salesorder.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.seller.repository.SellerRepository;
import br.com.toppower.erp_toppower.sales.quotation.entity.Quotation;
import br.com.toppower.erp_toppower.sales.quotation.entity.QuotationItem;
import br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus;
import br.com.toppower.erp_toppower.sales.quotation.exception.QuotationNotFoundException;
import br.com.toppower.erp_toppower.sales.quotation.repository.QuotationItemRepository;
import br.com.toppower.erp_toppower.sales.quotation.repository.QuotationRepository;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderCreateRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderFromQuotationRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderItemRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderResponse;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderSummaryResponse;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderUpdateRequest;
import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrder;
import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrderItem;
import br.com.toppower.erp_toppower.sales.salesorder.enums.SalesOrderStatus;
import br.com.toppower.erp_toppower.sales.salesorder.exception.InvalidSalesOrderClientException;
import br.com.toppower.erp_toppower.sales.salesorder.exception.QuotationAlreadyConvertedException;
import br.com.toppower.erp_toppower.sales.salesorder.exception.SalesOrderBusinessException;
import br.com.toppower.erp_toppower.sales.salesorder.exception.SalesOrderClientNotFoundException;
import br.com.toppower.erp_toppower.sales.salesorder.exception.SalesOrderNotFoundException;
import br.com.toppower.erp_toppower.sales.salesorder.mapper.SalesOrderMapper;
import br.com.toppower.erp_toppower.sales.salesorder.repository.SalesOrderItemRepository;
import br.com.toppower.erp_toppower.sales.salesorder.repository.SalesOrderRepository;
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
 * Regras de negócio do ciclo de vida de um pedido de venda.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>Gerar o número sequencial a partir de {@code 1000};</li>
 *   <li>Validar a invariante de cliente (exatamente um entre
 *       {@code customerUuid} e {@code companyUuid});</li>
 *   <li>Converter uma {@code Quotation} ATIVA em pedido (snapshot +
 *       rastreabilidade), marcando a proposta como {@code CONVERTIDA};</li>
 *   <li>Persistir o agregado (header + itens) garantindo que o
 *       {@code totalPrice} de cada item (líquido) seja calculado pelo
 *       mapper e os totais do pedido sejam recalculados antes da
 *       resposta;</li>
 *   <li>Listar com filtros (status, intervalo de datas, número, cliente,
 *       vendedor, número da proposta de origem);</li>
 *   <li>Avançar o status (ABERTO → EM_SEPARACAO → FATURADO → ENTREGUE)
 *       e cancelar (soft via status).</li>
 * </ul>
 *
 * <p><b>Sem margem de lucro</b> — o pedido é o documento externo enviado
 * ao cliente. A margem é informação interna mantida apenas na
 * {@code Quotation}.</p>
 */
@Service
public class SalesOrderService {

    /** Valor inicial da sequência (primeiro pedido emitido será {@code 1000}). */
    static final long INITIAL_NUMBER = 1000L;

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final SellerRepository sellerRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             SalesOrderItemRepository salesOrderItemRepository,
                             QuotationRepository quotationRepository,
                             QuotationItemRepository quotationItemRepository,
                             CustomerRepository customerRepository,
                             CompanyRepository companyRepository,
                             SellerRepository sellerRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.quotationRepository = quotationRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.sellerRepository = sellerRepository;
    }

    // ---------------------------------------------------------------------
    // Create (direto)
    // ---------------------------------------------------------------------

    @Transactional
    public SalesOrderResponse create(SalesOrderCreateRequest request) {
        validateClientReference(request.customerUuid(), request.companyUuid(), true);
        validateItemsConsistency(request.items());

        SalesOrder header = SalesOrderMapper.toEntity(request);
        header.setNumber(generateNextNumber());

        SalesOrder savedHeader = salesOrderRepository.save(header);

        List<SalesOrderItem> items = new ArrayList<>(request.items().size());
        for (SalesOrderItemRequest itemReq : request.items()) {
            items.add(salesOrderItemRepository.save(
                    SalesOrderMapper.toItemEntity(itemReq, savedHeader.getUuid())));
        }

        savedHeader.recalculateTotals(items);
        ClientResolved client = resolveClient(savedHeader);
        return SalesOrderMapper.toResponse(savedHeader, items, resolveSellerName(savedHeader),
                client.name(), client.code());
    }

    // ---------------------------------------------------------------------
    // Create (conversão de proposta)
    // ---------------------------------------------------------------------

    /**
     * Converte uma {@code Quotation} ATIVA em pedido de venda. Copia
     * cliente, vendedor, itens (snapshot), descontos, frete, condição
     * de pagamento e observações; aplica sobrescritas opcionais.
     * <b>Embuti a margem de lucro</b> da proposta nos preços dos itens,
     * de modo que o total do pedido reflita o valor com margem cobrado
     * do cliente. Marca a proposta como {@code CONVERTIDA}.
     *
     * @param quotationId UUID da proposta a converter
     * @param override    campos opcionais a sobrescrever (nulo para
     *                    copiar tudo da proposta)
     */
    @Transactional
    public SalesOrderResponse createFromQuotation(UUID quotationId, SalesOrderFromQuotationRequest override) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new QuotationNotFoundException(quotationId));

        if (quotation.getStatus() != QuotationStatus.ATIVA) {
            throw new QuotationAlreadyConvertedException(quotationId);
        }

        List<QuotationItem> quotationItems = quotationItemRepository
                .findByQuotationUuidOrderByCreatedAtAsc(quotationId);
        if (quotationItems == null || quotationItems.isEmpty()) {
            throw new SalesOrderBusinessException(
                    "Proposta " + quotation.getNumber() + " não possui itens para conversão.");
        }

        // A proposta já teve cliente validado na criação; revalida existência
        // apenas para consistência defensiva (não rejeita se ainda válido).
        validateClientReference(quotation.getCustomerUuid(), quotation.getCompanyUuid(), true);

        SalesOrder header = SalesOrderMapper.fromQuotation(quotation, quotationItems, override);
        header.setNumber(generateNextNumber());

        SalesOrder savedHeader = salesOrderRepository.save(header);

        List<SalesOrderItem> items = new ArrayList<>(quotationItems.size());
        for (QuotationItem qItem : quotationItems) {
            items.add(salesOrderItemRepository.save(
                    SalesOrderMapper.fromQuotationItem(qItem, savedHeader.getUuid(),
                            quotation.getProfitMargin())));
        }

        // Marca a proposta como CONVERTIDA — não pode ser reconvertida.
        quotation.setStatus(QuotationStatus.CONVERTIDA);
        quotationRepository.save(quotation);

        savedHeader.recalculateTotals(items);
        ClientResolved client = resolveClient(savedHeader);
        return SalesOrderMapper.toResponse(savedHeader, items, resolveSellerName(savedHeader),
                client.name(), client.code());
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SalesOrderResponse getById(UUID id) {
        SalesOrder o = salesOrderRepository.findById(id)
                .orElseThrow(() -> new SalesOrderNotFoundException(id));
        List<SalesOrderItem> items = salesOrderItemRepository
                .findBySalesOrderUuidOrderByCreatedAtAsc(id);
        o.recalculateTotals(items);
        ClientResolved client = resolveClient(o);
        return SalesOrderMapper.toResponse(o, items, resolveSellerName(o),
                client.name(), client.code());
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse getByNumber(Long number) {
        SalesOrder o = salesOrderRepository.findByNumber(number)
                .orElseThrow(() -> new SalesOrderNotFoundException(number));
        List<SalesOrderItem> items = salesOrderItemRepository
                .findBySalesOrderUuidOrderByCreatedAtAsc(o.getUuid());
        o.recalculateTotals(items);
        ClientResolved client = resolveClient(o);
        return SalesOrderMapper.toResponse(o, items, resolveSellerName(o),
                client.name(), client.code());
    }

    /**
     * Lista paginada com filtros opcionais. Todos os parâmetros são
     * opcionais; nulos significam "sem filtro".
     *
     * @param status           status do pedido (opcional)
     * @param startDate        data de emissão a partir de (opcional)
     * @param endDate          data de emissão até (opcional)
     * @param clientUuid       UUID do cliente (PF ou PJ) (opcional)
     * @param sellerUuid       UUID do vendedor (opcional)
     * @param numberLike      trecho do número (opcional)
     * @param quotationNumber número da proposta de origem (opcional)
     * @param pageable         paginação e ordenação
     */
    @Transactional(readOnly = true)
    public PagedResponse<SalesOrderSummaryResponse> search(SalesOrderStatus status,
                                                           LocalDate startDate,
                                                           LocalDate endDate,
                                                           UUID clientUuid,
                                                           UUID sellerUuid,
                                                           String numberLike,
                                                           Long quotationNumber,
                                                           Pageable pageable) {
        Specification<SalesOrder> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), endDate));
            }
            if (clientUuid != null) {
                jakarta.persistence.criteria.Predicate byCustomer =
                        cb.equal(root.get("customerUuid"), clientUuid);
                jakarta.persistence.criteria.Predicate byCompany =
                        cb.equal(root.get("companyUuid"), clientUuid);
                predicates.add(cb.or(byCustomer, byCompany));
            }
            if (sellerUuid != null) {
                predicates.add(cb.equal(root.get("sellerUuid"), sellerUuid));
            }
            if (numberLike != null && !numberLike.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("number")),
                        "%" + numberLike.toLowerCase().trim() + "%"));
            }
            if (quotationNumber != null) {
                predicates.add(cb.equal(root.get("quotationNumber"), quotationNumber));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<SalesOrder> page = salesOrderRepository.findAll(spec, pageable);

        Page<SalesOrderSummaryResponse> mapped = page.map(o -> {
            List<SalesOrderItem> items = salesOrderItemRepository
                    .findBySalesOrderUuidOrderByCreatedAtAsc(o.getUuid());
            o.recalculateTotals(items);
            ClientResolved client = resolveClient(o);
            String sellerName = resolveSellerName(o);
            return SalesOrderMapper.toSummary(o, client.name(), client.code(), sellerName);
        });
        return PagedResponse.from(mapped);
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Transactional
    public SalesOrderResponse update(UUID id, SalesOrderUpdateRequest request) {
        SalesOrder o = salesOrderRepository.findById(id)
                .orElseThrow(() -> new SalesOrderNotFoundException(id));

        if (isImmutable(o.getStatus())) {
            throw new SalesOrderBusinessException(
                    "Pedido em status " + o.getStatus() + " não pode ser alterado.");
        }

        UUID effectiveCustomer = (request.customerUuid() != null)
                ? request.customerUuid() : o.getCustomerUuid();
        UUID effectiveCompany = (request.companyUuid() != null)
                ? request.companyUuid() : o.getCompanyUuid();
        validateClientReference(effectiveCustomer, effectiveCompany, false);

        if (request.items() != null) {
            validateItemsConsistency(request.items());
            salesOrderItemRepository.deleteBySalesOrderUuid(id);
            salesOrderItemRepository.flush();
        }

        SalesOrderMapper.applyUpdate(o, request);
        SalesOrder saved = salesOrderRepository.save(o);

        List<SalesOrderItem> items;
        if (request.items() != null) {
            items = new ArrayList<>(request.items().size());
            for (SalesOrderItemRequest itemReq : request.items()) {
                items.add(salesOrderItemRepository.save(
                        SalesOrderMapper.toItemEntity(itemReq, saved.getUuid())));
            }
        } else {
            items = salesOrderItemRepository.findBySalesOrderUuidOrderByCreatedAtAsc(id);
        }

        saved.recalculateTotals(items);
        ClientResolved client = resolveClient(saved);
        return SalesOrderMapper.toResponse(saved, items, resolveSellerName(saved),
                client.name(), client.code());
    }

    // ---------------------------------------------------------------------
    // Transições de status
    // ---------------------------------------------------------------------

    /**
     * Avança o status do pedido para o próximo estado do ciclo:
     * {@code ABERTO → EM_SEPARACAO → FATURADO → ENTREGUE}. Pular etapas
     * ou avançar a partir de estado terminal lança 409.
     */
    @Transactional
    public SalesOrderResponse advanceStatus(UUID id) {
        SalesOrder o = salesOrderRepository.findById(id)
                .orElseThrow(() -> new SalesOrderNotFoundException(id));

        SalesOrderStatus next = nextStatus(o.getStatus());
        if (next == null) {
            throw new SalesOrderBusinessException(
                    "Não há próximo status a partir de " + o.getStatus() + ".");
        }
        o.setStatus(next);
        SalesOrder saved = salesOrderRepository.save(o);

        List<SalesOrderItem> items = salesOrderItemRepository
                .findBySalesOrderUuidOrderByCreatedAtAsc(id);
        saved.recalculateTotals(items);
        ClientResolved client = resolveClient(saved);
        return SalesOrderMapper.toResponse(saved, items, resolveSellerName(saved),
                client.name(), client.code());
    }

    // ---------------------------------------------------------------------
    // Cancel (soft via status)
    // ---------------------------------------------------------------------

    @Transactional
    public SalesOrderResponse cancel(UUID id) {
        SalesOrder o = salesOrderRepository.findById(id)
                .orElseThrow(() -> new SalesOrderNotFoundException(id));

        if (o.getStatus() == SalesOrderStatus.CANCELADO) {
            throw new SalesOrderBusinessException("Pedido já está cancelado.");
        }
        if (o.getStatus() == SalesOrderStatus.FATURADO
                || o.getStatus() == SalesOrderStatus.ENTREGUE) {
            throw new SalesOrderBusinessException(
                    "Pedido " + o.getStatus() + " não pode ser cancelado.");
        }
        o.setStatus(SalesOrderStatus.CANCELADO);
        SalesOrder saved = salesOrderRepository.save(o);

        List<SalesOrderItem> items = salesOrderItemRepository
                .findBySalesOrderUuidOrderByCreatedAtAsc(id);
        saved.recalculateTotals(items);
        ClientResolved client = resolveClient(saved);
        return SalesOrderMapper.toResponse(saved, items, resolveSellerName(saved),
                client.name(), client.code());
    }

    // ---------------------------------------------------------------------
    // Utilitários públicos
    // ---------------------------------------------------------------------

    /**
     * Retorna o próximo número que seria atribuído a um novo pedido,
     * sem persistir nada. Útil para o frontend exibir o valor previsto
     * no formulário antes do envio.
     */
    @Transactional(readOnly = true)
    public Long getNextNumber() {
        return generateNextNumber();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Long generateNextNumber() {
        Long maxNumber = salesOrderRepository.findMaxNumber();
        if (maxNumber == null) {
            return INITIAL_NUMBER;
        }
        return maxNumber + 1L;
    }

    /**
     * Valida a invariante do cliente: exatamente um entre
     * {@code customerUuid} e {@code companyUuid} deve estar preenchido.
     * Quando {@code verifyExists} é verdadeiro, também verifica se o
     * cliente existe no banco.
     */
    private void validateClientReference(UUID customerUuid, UUID companyUuid, boolean verifyExists) {
        if (customerUuid == null && companyUuid == null) {
            throw InvalidSalesOrderClientException.bothNull();
        }
        if (customerUuid != null && companyUuid != null) {
            throw InvalidSalesOrderClientException.bothSet();
        }
        if (!verifyExists) {
            return;
        }
        if (customerUuid != null && !customerRepository.existsById(customerUuid)) {
            throw new SalesOrderClientNotFoundException(customerUuid, "CUSTOMER");
        }
        if (companyUuid != null && !companyRepository.existsById(companyUuid)) {
            throw new SalesOrderClientNotFoundException(companyUuid, "COMPANY");
        }
    }

    /**
     * Valida regras de negócio dos itens: quantidade, preço, e
     * consistência entre {@code discount} e {@code discountType}.
     */
    private void validateItemsConsistency(List<SalesOrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new SalesOrderBusinessException("O pedido deve ter ao menos um item.");
        }
        for (int i = 0; i < items.size(); i++) {
            SalesOrderItemRequest it = items.get(i);
            if (it.discount() != null && it.discountType() == null) {
                throw new SalesOrderBusinessException(
                        "Item #" + (i + 1) + ": discountType é obrigatório quando discount é informado.");
            }
            if (it.discountType() != null && (it.discount() == null || it.discount().signum() == 0)) {
                throw new SalesOrderBusinessException(
                        "Item #" + (i + 1) + ": discount é obrigatório quando discountType é informado.");
            }
        }
    }

    /**
     * Resolve o nome e o código de exibição do cliente referenciado pelo
     * pedido (PF: nome; PJ: nome fantasia se houver, senão razão social).
     * Retorna {@code null} em ambos os campos quando o registro não existe
     * mais (inativado/removido), mantendo o UUID como referência no DTO —
     * mesmo tratamento dado a {@link #resolveSellerName(SalesOrder)}.
     */
    private ClientResolved resolveClient(SalesOrder o) {
        if (o.getCustomerUuid() != null) {
            return customerRepository.findById(o.getCustomerUuid())
                    .map(c -> new ClientResolved(c.getName(), c.getCode()))
                    .orElse(ClientResolved.EMPTY);
        }
        if (o.getCompanyUuid() != null) {
            return companyRepository.findById(o.getCompanyUuid())
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
     * Par (nome, código) resolvido a partir do cliente referenciado pelo
     * pedido. Usado para popular {@code clientName} e {@code clientCode}
     * no {@link SalesOrderResponse} e {@link SalesOrderSummaryResponse}.
     */
    private record ClientResolved(String name, String code) {
        static final ClientResolved EMPTY = new ClientResolved(null, null);
    }

    /**
     * Resolve o nome do vendedor referenciado pelo pedido. Retorna
     * {@code null} quando o vendedor não existe mais (inativado/removido),
     * mantendo o UUID como referência no DTO.
     */
    private String resolveSellerName(SalesOrder o) {
        if (o.getSellerUuid() == null) {
            return null;
        }
        return sellerRepository.findById(o.getSellerUuid())
                .map(s -> s.getName())
                .orElse(null);
    }

    /**
     * Estados terminais/imutáveis: não podem ser editados via PATCH.
     */
    private boolean isImmutable(SalesOrderStatus status) {
        return status == SalesOrderStatus.FATURADO
                || status == SalesOrderStatus.ENTREGUE
                || status == SalesOrderStatus.CANCELADO;
    }

    /**
     * Próximo estado válido no ciclo. Retorna {@code null} quando não
     * há avanço possível (estado terminal ou cancelado).
     */
    private SalesOrderStatus nextStatus(SalesOrderStatus current) {
        if (current == null) {
            return null;
        }
        return switch (current) {
            case ABERTO -> SalesOrderStatus.EM_SEPARACAO;
            case EM_SEPARACAO -> SalesOrderStatus.FATURADO;
            case FATURADO -> SalesOrderStatus.ENTREGUE;
            case ENTREGUE, CANCELADO -> null;
        };
    }
}