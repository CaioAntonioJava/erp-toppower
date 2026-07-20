package br.com.toppower.erp_toppower.receivable.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableCreateRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivablePaymentRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableSummaryResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableUpdateRequest;
import br.com.toppower.erp_toppower.receivable.entity.Receivable;
import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import br.com.toppower.erp_toppower.receivable.exception.InvalidReceivableClientException;
import br.com.toppower.erp_toppower.receivable.exception.ReceivableBusinessException;
import br.com.toppower.erp_toppower.receivable.exception.ReceivableNotFoundException;
import br.com.toppower.erp_toppower.receivable.exception.ReceivablePaymentNotFoundException;
import br.com.toppower.erp_toppower.receivable.mapper.ReceivableMapper;
import br.com.toppower.erp_toppower.receivable.repository.ReceivablePaymentRepository;
import br.com.toppower.erp_toppower.receivable.repository.ReceivableRepository;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrder;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Regras de negócio do módulo de contas a receber.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>CRUD manual (create, list, getById, update, cancelar, reativar);</li>
 *   <li>Registro e remoção de pagamentos avulsos, com recálculo automático
 *       do {@code paidAmount} e do status (ABERTO ↔ PAGO);</li>
 *   <li>Geração automática de contas a partir de documentos de origem
 *       (pedido de venda, proposta técnica, contrato);</li>
 *   <li>Cancelamento defensivo da conta vinculada quando um documento
 *       de origem é reaberto — bloqueando a reabertura se a conta já
 *       tiver pagamentos registrados.</li>
 * </ul>
 */
@Service
public class ReceivableService {

    private static final Logger log = LoggerFactory.getLogger(ReceivableService.class);
    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final ReceivableRepository repository;
    private final ReceivablePaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;

    public ReceivableService(ReceivableRepository repository,
                             ReceivablePaymentRepository paymentRepository,
                             CustomerRepository customerRepository,
                             CompanyRepository companyRepository) {
        this.repository = repository;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
    }

    // ---------------------------------------------------------------------
    // Create (manual)
    // ---------------------------------------------------------------------

    @Transactional
    public ReceivableResponse create(ReceivableCreateRequest request) {
        validateClientReference(request.customerId(), request.companyId(), true);
        Receivable r = ReceivableMapper.toEntity(request);
        Receivable saved = repository.save(r);
        return toResponseWithPayments(saved);
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    /**
     * Lista paginada com filtros opcionais. Todos os parâmetros são
     * opcionais; nulos significam "sem filtro".
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReceivableSummaryResponse> search(ReceivableStatus status,
                                                           ReceivableSource sourceType,
                                                           Long clientId,
                                                           LocalDate dueFrom,
                                                           LocalDate dueTo,
                                                           String query,
                                                           Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Specification<Receivable> spec = (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (sourceType != null) {
                predicates.add(cb.equal(root.get("sourceType"), sourceType));
            }
            if (clientId != null) {
                jakarta.persistence.criteria.Predicate byCustomer =
                        cb.equal(root.get("customerId"), clientId);
                jakarta.persistence.criteria.Predicate byCompany =
                        cb.equal(root.get("companyId"), clientId);
                predicates.add(cb.or(byCustomer, byCompany));
            }
            if (dueFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueFrom));
            }
            if (dueTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueTo));
            }
            if (trimmed != null && !trimmed.isEmpty()) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")),
                                "%" + trimmed.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("contractCode")),
                                "%" + trimmed.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("technicalProposalCode")),
                                "%" + trimmed.toLowerCase() + "%")
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Receivable> page = repository.findAll(spec, pageable);
        Page<ReceivableSummaryResponse> mapped = page.map(r -> {
            ClientResolved client = resolveClient(r);
            return ReceivableMapper.toSummary(r, client.name(), client.code());
        });
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public ReceivableResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponseWithPayments)
                .orElseThrow(() -> new ReceivableNotFoundException(id));
    }

    // ---------------------------------------------------------------------
    // Update / Cancel / Activate
    // ---------------------------------------------------------------------

    @Transactional
    public ReceivableResponse update(Long id, ReceivableUpdateRequest request) {
        Receivable r = repository.findById(id)
                .orElseThrow(() -> new ReceivableNotFoundException(id));
        if (r.getStatus() == ReceivableStatus.PAGO) {
            throw ReceivableBusinessException.cannotModifyPaid();
        }
        if (r.getStatus() == ReceivableStatus.CANCELADO) {
            throw new ReceivableBusinessException(
                    "Conta CANCELADA não pode ser alterada. Reative a conta antes de editar.");
        }
        ReceivableMapper.applyUpdate(r, request);
        Receivable saved = repository.save(r);
        return toResponseWithPayments(saved);
    }

    /**
     * Soft delete: marca a conta como CANCELADA. Bloqueada para contas
     * PAGO (precisa reabrir/manter conforme necessário).
     */
    @Transactional
    public void cancel(Long id) {
        Receivable r = repository.findById(id)
                .orElseThrow(() -> new ReceivableNotFoundException(id));
        if (r.getStatus() == ReceivableStatus.PAGO) {
            throw ReceivableBusinessException.cannotModifyPaid();
        }
        r.setStatus(ReceivableStatus.CANCELADO);
        repository.save(r);
    }

    /**
     * Reativa uma conta CANCELADA, voltando para ABERTO. Recalcula o
     * status com base no paidAmount (volta para PAGO se já quitada).
     */
    @Transactional
    public ReceivableResponse activate(Long id) {
        Receivable r = repository.findById(id)
                .orElseThrow(() -> new ReceivableNotFoundException(id));
        if (r.getStatus() != ReceivableStatus.CANCELADO) {
            throw new ReceivableBusinessException(
                    "Apenas contas CANCELADAS podem ser reativadas. Status atual: "
                            + r.getStatus());
        }
        recomputeStatusFromPayments(r);
        Receivable saved = repository.save(r);
        return toResponseWithPayments(saved);
    }

    // ---------------------------------------------------------------------
    // Pagamentos
    // ---------------------------------------------------------------------

    /**
     * Registra um pagamento avulso contra a conta. Recalcula paidAmount e
     * transita para PAGO quando o saldo devedor zera.
     */
    @Transactional
    public ReceivableResponse registerPayment(Long receivableId, ReceivablePaymentRequest request) {
        Receivable r = repository.findById(receivableId)
                .orElseThrow(() -> new ReceivableNotFoundException(receivableId));
        if (r.getStatus() != ReceivableStatus.ABERTO) {
            throw ReceivableBusinessException.notOpenForPayment();
        }

        BigDecimal currentPaid = (r.getPaidAmount() != null) ? r.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = r.getValue().subtract(currentPaid);
        if (request.amount().compareTo(balance) > 0) {
            throw ReceivableBusinessException.paymentExceedsBalance(request.amount(), balance);
        }

        ReceivablePayment payment = ReceivableMapper.toPaymentEntity(receivableId, request);
        paymentRepository.save(payment);

        recomputePaidAmount(r);
        recomputeStatusFromPayments(r);
        recomputePaymentDate(r);

        Receivable saved = repository.save(r);
        return toResponseWithPayments(saved);
    }

    /**
     * Remove um pagamento e recalcula paidAmount/status. Se a conta volta
     * a ter saldo devedor, status vira ABERTO novamente.
     */
    @Transactional
    public ReceivableResponse removePayment(Long receivableId, Long paymentId) {
        Receivable r = repository.findById(receivableId)
                .orElseThrow(() -> new ReceivableNotFoundException(receivableId));
        if (r.getStatus() == ReceivableStatus.CANCELADO) {
            throw new ReceivableBusinessException(
                    "Conta CANCELADA não permite remover pagamentos.");
        }
        ReceivablePayment payment = paymentRepository.findByIdAndReceivableId(paymentId, receivableId)
                .orElseThrow(() -> new ReceivablePaymentNotFoundException(paymentId, receivableId));
        paymentRepository.delete(payment);
        paymentRepository.flush();

        recomputePaidAmount(r);
        recomputeStatusFromPayments(r);
        recomputePaymentDate(r);

        Receivable saved = repository.save(r);
        return toResponseWithPayments(saved);
    }

    @Transactional(readOnly = true)
    public List<ReceivablePayment> listPayments(Long receivableId) {
        if (!repository.existsById(receivableId)) {
            throw new ReceivableNotFoundException(receivableId);
        }
        return paymentRepository.findByReceivableIdOrderByPaymentDateAsc(receivableId);
    }

    // ---------------------------------------------------------------------
    // Integrações — geração automática a partir de documentos de origem
    // ---------------------------------------------------------------------

    /**
     * Gera uma conta a receber a partir de um pedido de venda (típico:
     * conversão de proposta em pedido). Usa o total do pedido (recebido
     * como parâmetro, pois é um campo {@code @Transient} calculado em
     * memória pelo SalesOrderService) e a condição de pagamento (enum)
     * para o vencimento.
     */
    @Transactional
    public Optional<Receivable> generateFromSalesOrder(SalesOrder order, BigDecimal total) {
        if (order == null || total == null || total.signum() <= 0) {
            return Optional.empty();
        }
        // Não gera duplicata: se já existe conta ativa para o pedido, skip.
        Optional<Receivable> existing = repository.findActiveBySalesOrderId(order.getId());
        if (existing.isPresent()) {
            log.warn("Conta a receber já existe para o pedido {}. Pulando geração.", order.getId());
            return existing;
        }
        if (order.getCustomerId() == null && order.getCompanyId() == null) {
            log.warn("Pedido {} sem cliente vinculado — conta a receber não gerada.", order.getId());
            return Optional.empty();
        }

        Receivable r = new Receivable();
        r.setDescription("Pedido de Venda " + order.formattedCode());
        r.setValue(total);
        r.setSourceType(ReceivableSource.SALES_ORDER);
        r.setCustomerId(order.getCustomerId());
        r.setCompanyId(order.getCompanyId());
        r.setSalesOrderId(order.getId());
        r.setSalesOrderNumber(order.getSequence());
        r.setSalesOrderCode(order.formattedCode());
        r.setPaymentCondition(order.getPaymentCondition());
        LocalDate base = (order.getOrderDate() != null) ? order.getOrderDate() : LocalDate.now();
        int days = PaymentConditionParser.firstTermDays(order.getPaymentCondition());
        r.setDueDate(base.plusDays(days));
        return Optional.of(repository.save(r));
    }

    /**
     * Gera uma conta a receber ao concluir uma proposta técnica. O total
     * é recebido como parâmetro (campo {@code @Transient} calculado em
     * memória pelo TechnicalProposalService).
     */
    @Transactional
    public Optional<Receivable> generateFromTechnicalProposal(TechnicalProposal proposal, BigDecimal total) {
        if (proposal == null || total == null || total.signum() <= 0) {
            return Optional.empty();
        }
        Optional<Receivable> existing = repository.findActiveByTechnicalProposalId(proposal.getId());
        if (existing.isPresent()) {
            log.warn("Conta a receber já existe para a proposta técnica {}. Pulando geração.",
                    proposal.getId());
            return existing;
        }
        if (proposal.getCustomerId() == null && proposal.getCompanyId() == null) {
            log.warn("Proposta técnica {} sem cliente vinculado — conta a receber não gerada.",
                    proposal.getId());
            return Optional.empty();
        }

        Receivable r = new Receivable();
        r.setDescription("Proposta Técnica " + proposal.formattedCode());
        r.setValue(total);
        r.setSourceType(ReceivableSource.TECHNICAL_PROPOSAL);
        r.setCustomerId(proposal.getCustomerId());
        r.setCompanyId(proposal.getCompanyId());
        r.setTechnicalProposalId(proposal.getId());
        r.setTechnicalProposalCode(proposal.formattedCode());
        r.setPaymentCondition(proposal.getPaymentCondition());
        int days = PaymentConditionParser.firstTermDays(proposal.getPaymentCondition());
        r.setDueDate(LocalDate.now().plusDays(days));
        return Optional.of(repository.save(r));
    }

    /**
     * Gera uma conta a receber ao concluir um contrato.
     */
    @Transactional
    public Optional<Receivable> generateFromContract(Contract contract) {
        if (contract == null || contract.getPrice() == null) {
            return Optional.empty();
        }
        Optional<Receivable> existing = repository.findActiveByContractId(contract.getId());
        if (existing.isPresent()) {
            log.warn("Conta a receber já existe para o contrato {}. Pulando geração.",
                    contract.getId());
            return existing;
        }
        if (contract.getCustomerId() == null && contract.getCompanyId() == null) {
            log.warn("Contrato {} sem cliente vinculado — conta a receber não gerada.",
                    contract.getId());
            return Optional.empty();
        }

        Receivable r = new Receivable();
        r.setDescription("Contrato " + contract.formattedCode());
        r.setValue(contract.getPrice());
        r.setSourceType(ReceivableSource.CONTRACT);
        r.setCustomerId(contract.getCustomerId());
        r.setCompanyId(contract.getCompanyId());
        r.setContractId(contract.getId());
        r.setContractCode(contract.formattedCode());
        // Contrato não possui paymentCondition — fallback 30 dias.
        int days = PaymentConditionParser.firstTermDays((String) null);
        r.setDueDate(LocalDate.now().plusDays(days));
        return Optional.of(repository.save(r));
    }

    /**
     * Cancela a conta a receber vinculada a um documento de origem quando
     * este é reaberto. <b>Bloqueia</b> a reabertura (lança
     * {@link ReceivableBusinessException#cannotReopenWithPayments}) se a
     * conta já tiver pagamentos registrados — o usuário precisa tratar a
     * conta manualmente antes.
     *
     * <p>Tolerante à ausência: se não houver conta vinculada (documento
     * criado antes do módulo), retorna silenciosamente.</p>
     *
     * @param receivableId ID da conta vinculada (deve ser buscado antes
     *                     pelo service chamador via
     *                     {@code findActiveBySalesOrderId/TechnicalProposalId/ContractId})
     * @param documentLabel rótulo do documento para a mensagem de erro
     *                       (ex.: "o contrato CL-001-2026")
     */
    @Transactional
    public void cancelIfNoPayments(Long receivableId, String documentLabel) {
        if (receivableId == null) {
            return;
        }
        Receivable r = repository.findById(receivableId)
                .orElse(null);
        if (r == null) {
            return;
        }
        BigDecimal paid = paymentRepository.sumAmountByReceivableId(receivableId);
        if (paid != null && paid.signum() > 0) {
            throw ReceivableBusinessException.cannotReopenWithPayments(documentLabel);
        }
        r.setStatus(ReceivableStatus.CANCELADO);
        repository.save(r);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Recalcula {@code paidAmount} a partir da soma dos pagamentos no banco.
     */
    private void recomputePaidAmount(Receivable r) {
        BigDecimal sum = paymentRepository.sumAmountByReceivableId(r.getId());
        r.setPaidAmount(sum != null ? sum : BigDecimal.ZERO);
    }

    /**
     * Ajusta o status com base no paidAmount. PAGO quando
     * {@code paidAmount >= value}; ABERTO caso contrário. Não altera
     * CANCELADO (apenas {@link #activate} sai de CANCELADO).
     */
    private void recomputeStatusFromPayments(Receivable r) {
        if (r.getStatus() == ReceivableStatus.CANCELADO) {
            return;
        }
        BigDecimal paid = (r.getPaidAmount() != null) ? r.getPaidAmount() : BigDecimal.ZERO;
        if (paid.compareTo(r.getValue()) >= 0) {
            r.setStatus(ReceivableStatus.PAGO);
        } else {
            r.setStatus(ReceivableStatus.ABERTO);
        }
    }

    /**
     * Atualiza {@code paymentDate} com a data do pagamento mais recente
     * (ou null se não houver pagamentos).
     */
    private void recomputePaymentDate(Receivable r) {
        List<ReceivablePayment> payments =
                paymentRepository.findByReceivableIdOrderByPaymentDateAsc(r.getId());
        if (payments.isEmpty()) {
            r.setPaymentDate(null);
        } else {
            r.setPaymentDate(payments.get(payments.size() - 1).getPaymentDate());
        }
    }

    private ReceivableResponse toResponseWithPayments(Receivable r) {
        List<ReceivablePayment> payments =
                paymentRepository.findByReceivableIdOrderByPaymentDateAsc(r.getId());
        ClientResolved client = resolveClient(r);
        return ReceivableMapper.toResponse(r, client.name(), client.code(), payments);
    }

    private void validateClientReference(Long customerId, Long companyId, boolean verifyExists) {
        if (customerId == null && companyId == null) {
            throw InvalidReceivableClientException.bothNull();
        }
        if (customerId != null && companyId != null) {
            throw InvalidReceivableClientException.bothSet();
        }
        if (!verifyExists) {
            return;
        }
        if (customerId != null && !customerRepository.existsById(customerId)) {
            throw new ReceivableBusinessException("Cliente (pessoa física) não encontrado: " + customerId);
        }
        if (companyId != null && !companyRepository.existsById(companyId)) {
            throw new ReceivableBusinessException("Empresa (pessoa jurídica) não encontrada: " + companyId);
        }
    }

    /**
     * Resolve nome e código de exibição do cliente/empresa devedor.
     */
    private ClientResolved resolveClient(Receivable r) {
        if (r.getCustomerId() != null) {
            return customerRepository.findById(r.getCustomerId())
                    .map(c -> new ClientResolved(c.getName(), c.getCode()))
                    .orElse(ClientResolved.EMPTY);
        }
        if (r.getCompanyId() != null) {
            return companyRepository.findById(r.getCompanyId())
                    .map(c -> new ClientResolved(
                            (c.getTradeName() != null && !c.getTradeName().isBlank())
                                    ? c.getTradeName()
                                    : c.getLegalName(),
                            c.getCode()))
                    .orElse(ClientResolved.EMPTY);
        }
        return ClientResolved.EMPTY;
    }

    private record ClientResolved(String name, String code) {
        static final ClientResolved EMPTY = new ClientResolved(null, null);
    }
}