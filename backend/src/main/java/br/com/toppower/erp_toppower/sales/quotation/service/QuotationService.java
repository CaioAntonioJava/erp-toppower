package br.com.toppower.erp_toppower.sales.quotation.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationCreateRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationItemRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Regras de negócio do ciclo de vida de uma proposta comercial.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>Gerar o número sequencial a partir de {@code QUO001500};</li>
 *   <li>Validar a invariante de cliente (exatamente um entre
 *       {@code customerUuid} e {@code companyUuid});</li>
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

    public QuotationService(QuotationRepository quotationRepository,
                            QuotationItemRepository quotationItemRepository,
                            CustomerRepository customerRepository,
                            CompanyRepository companyRepository) {
        this.quotationRepository = quotationRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Transactional
    public QuotationResponse create(QuotationCreateRequest request) {
        validateClientReference(request.customerUuid(), request.companyUuid(), true);
        validateItemsConsistency(request.items(), null);

        Quotation header = QuotationMapper.toEntity(request);
        header.setNumber(generateNextNumber());

        // Persiste o header para obter o UUID que será referenciado pelos itens
        Quotation savedHeader = quotationRepository.save(header);

        // Persiste os itens, agora com o UUID da proposta
        List<QuotationItem> items = new ArrayList<>(request.items().size());
        for (QuotationItemRequest itemReq : request.items()) {
            items.add(quotationItemRepository.save(
                    QuotationMapper.toItemEntity(itemReq, savedHeader.getUuid())));
        }

        // Recalcula e aplica os totais na entidade em memória (somente para a resposta)
        savedHeader.recalculateTotals(items);

        return QuotationMapper.toResponse(savedHeader, items);
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public QuotationResponse getById(UUID id) {
        Quotation q = quotationRepository.findById(id)
                .orElseThrow(() -> new QuotationNotFoundException(id));
        List<QuotationItem> items = quotationItemRepository
                .findByQuotationUuidOrderByCreatedAtAsc(id);
        q.recalculateTotals(items);
        return QuotationMapper.toResponse(q, items);
    }

    @Transactional(readOnly = true)
    public QuotationResponse getByNumber(Long number) {
        Quotation q = quotationRepository.findByNumber(number)
                .orElseThrow(() -> new QuotationNotFoundException(number));
        List<QuotationItem> items = quotationItemRepository
                .findByQuotationUuidOrderByCreatedAtAsc(q.getUuid());
        q.recalculateTotals(items);
        return QuotationMapper.toResponse(q, items);
    }

    /**
     * Lista paginada com filtros opcionais. Todos os parâmetros são
     * opcionais; nulos significam "sem filtro".
     *
     * @param status      status da proposta (opcional)
     * @param startDate   data de emissão a partir de (opcional)
     * @param endDate     data de emissão até (opcional)
     * @param clientUuid  UUID do cliente (PF ou PJ) (opcional)
     * @param sellerUuid  UUID do vendedor (opcional)
     * @param numberLike  trecho do número (opcional)
     * @param pageable    paginação e ordenação
     */
    @Transactional(readOnly = true)
    public PagedResponse<QuotationSummaryResponse> search(QuotationStatus status,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          UUID clientUuid,
                                                          UUID sellerUuid,
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
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Quotation> page = quotationRepository.findAll(spec, pageable);

        // Carrega itens e calcula totais por proposta
        Page<QuotationSummaryResponse> mapped = page.map(q -> {
            List<QuotationItem> items = quotationItemRepository
                    .findByQuotationUuidOrderByCreatedAtAsc(q.getUuid());
            q.recalculateTotals(items);
            String clientName = resolveClientName(q);
            return QuotationMapper.toSummary(q, clientName);
        });
        return PagedResponse.from(mapped);
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Transactional
    public QuotationResponse update(UUID id, QuotationUpdateRequest request) {
        Quotation q = quotationRepository.findById(id)
                .orElseThrow(() -> new QuotationNotFoundException(id));

        if (q.getStatus() == QuotationStatus.CONVERTIDA) {
            throw new QuotationBusinessException(
                    "Proposta CONVERTIDA em pedido não pode ser alterada.");
        }

        // Recalcula o cliente efetivo após o PATCH e valida
        UUID effectiveCustomer = (request.customerUuid() != null)
                ? request.customerUuid() : q.getCustomerUuid();
        UUID effectiveCompany = (request.companyUuid() != null)
                ? request.companyUuid() : q.getCompanyUuid();
        validateClientReference(effectiveCustomer, effectiveCompany, false);

        // Se a lista de itens foi enviada, valida e substitui por completo
        if (request.items() != null) {
            validateItemsConsistency(request.items(), null);
            quotationItemRepository.deleteByQuotationUuid(id);
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
                        QuotationMapper.toItemEntity(itemReq, saved.getUuid())));
            }
        } else {
            items = quotationItemRepository.findByQuotationUuidOrderByCreatedAtAsc(id);
        }

        saved.recalculateTotals(items);
        return QuotationMapper.toResponse(saved, items);
    }

    // ---------------------------------------------------------------------
    // Cancel (soft via status)
    // ---------------------------------------------------------------------

    @Transactional
    public QuotationResponse cancel(UUID id) {
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
                .findByQuotationUuidOrderByCreatedAtAsc(id);
        saved.recalculateTotals(items);
        return QuotationMapper.toResponse(saved, items);
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
        return generateNextNumber();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Long generateNextNumber() {
        Long maxNumber = quotationRepository.findMaxNumber();
        if (maxNumber == null) {
            // Nenhuma proposta cadastrada — inicia em 1500
            return INITIAL_NUMBER;
        }
        return maxNumber + 1L;
    }

    /**
     * Valida a invariante do cliente: exatamente um entre
     * {@code customerUuid} e {@code companyUuid} deve estar preenchido.
     * Quando {@code verifyExists} é verdadeiro (criação), também
     * verifica se o cliente existe no banco.
     */
    private void validateClientReference(UUID customerUuid, UUID companyUuid, boolean verifyExists) {
        if (customerUuid == null && companyUuid == null) {
            throw InvalidQuotationClientException.bothNull();
        }
        if (customerUuid != null && companyUuid != null) {
            throw InvalidQuotationClientException.bothSet();
        }
        if (!verifyExists) {
            return;
        }
        if (customerUuid != null && !customerRepository.existsById(customerUuid)) {
            throw new QuotationClientNotFoundException(customerUuid, "CUSTOMER");
        }
        if (companyUuid != null && !companyRepository.existsById(companyUuid)) {
            throw new QuotationClientNotFoundException(companyUuid, "COMPANY");
        }
    }

    /**
     * Valida regras de negócio dos itens: quantidade, preço, e
     * consistência entre {@code discount} e {@code discountType}.
     *
     * @param items     itens a validar
     * @param currentQuotationUuid  UUID da proposta (não utilizado por
     *                               enquanto, reservado para validações
     *                               futuras como unicidade de produto)
     */
    private void validateItemsConsistency(List<QuotationItemRequest> items, UUID currentQuotationUuid) {
        if (items == null || items.isEmpty()) {
            throw new QuotationBusinessException("A proposta deve ter ao menos um item.");
        }
        for (int i = 0; i < items.size(); i++) {
            QuotationItemRequest it = items.get(i);
            if (it.discount() != null && it.discountType() == null) {
                throw new QuotationBusinessException(
                        "Item #" + (i + 1) + ": discountType é obrigatório quando discount é informado.");
            }
            if (it.discountType() != null && (it.discount() == null || it.discount().signum() == 0)) {
                throw new QuotationBusinessException(
                        "Item #" + (i + 1) + ": discount é obrigatório quando discountType é informado.");
            }
        }
    }

    /**
     * Resolve o nome de exibição do cliente referenciado pela proposta
     * (PF: nome; PJ: nome fantasia se houver, senão razão social).
     */
    private String resolveClientName(Quotation q) {
        if (q.getCustomerUuid() != null) {
            return customerRepository.findById(q.getCustomerUuid())
                    .map(c -> c.getName())
                    .orElse(null);
        }
        if (q.getCompanyUuid() != null) {
            return companyRepository.findById(q.getCompanyUuid())
                    .map(c -> c.getTradeName() != null && !c.getTradeName().isBlank()
                            ? c.getTradeName()
                            : c.getLegalName())
                    .orElse(null);
        }
        return null;
    }
}
