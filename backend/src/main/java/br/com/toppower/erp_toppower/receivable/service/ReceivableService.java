package br.com.toppower.erp_toppower.receivable.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.receivable.dto.GenerateInstallmentsRequest;
import br.com.toppower.erp_toppower.receivable.dto.PreviewInstallmentsRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableCreateRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableInstallmentPreviewResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableInstallmentRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivablePaymentRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableSummaryResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableUpdateRequest;
import br.com.toppower.erp_toppower.receivable.entity.Receivable;
import br.com.toppower.erp_toppower.receivable.entity.ReceivableInstallment;
import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import br.com.toppower.erp_toppower.receivable.exception.InvalidReceivableClientException;
import br.com.toppower.erp_toppower.receivable.exception.ReceivableBusinessException;
import br.com.toppower.erp_toppower.receivable.exception.ReceivableInstallmentNotFoundException;
import br.com.toppower.erp_toppower.receivable.exception.ReceivableNotFoundException;
import br.com.toppower.erp_toppower.receivable.exception.ReceivablePaymentNotFoundException;
import br.com.toppower.erp_toppower.receivable.mapper.ReceivableMapper;
import br.com.toppower.erp_toppower.receivable.repository.ReceivableInstallmentRepository;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Regras de negócio do módulo de contas a receber.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>CRUD manual (create, search, getById, update, cancelar, reativar);</li>
 *   <li>Geração automática de parcelas programadas a partir da condição
 *       de pagamento (quando as parcelas explícitas não são informadas)
 *       ou uso das parcelas explícitas informadas pelo usuário;</li>
 *   <li>Ação pós-criação "Gerar parcelas" para particionar uma conta
 *       de parcela única em múltiplas parcelas a partir da condição;</li>
 *   <li>Preview de parcelas (cálculo puro, sem persistir);</li>
 *   <li>Registro e remoção de pagamentos avulsos contra parcelas, com
 *       recálculo automático do {@code paidAmount} da parcela, do
 *       {@code paidAmount} da conta e dos status (parcela → PAGO quando
 *       quitada; conta → PAGO quando todas as parcelas quitadas);</li>
 *   <li>Baixa total de uma parcela ou de todas as parcelas abertas;</li>
 *   <li>Geração automática de conta a receber a partir de documentos de
 *       origem (pedido de venda, proposta técnica, contrato).</li>
 * </ul>
 */
@Service
public class ReceivableService {

    private static final Logger log = LoggerFactory.getLogger(ReceivableService.class);
    private static final int MIN_SEARCH_QUERY_LENGTH = 2;
    private static final int SCALE = 2;

    private final ReceivableRepository repository;
    private final ReceivableInstallmentRepository installmentRepository;
    private final ReceivablePaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;

    public ReceivableService(ReceivableRepository repository,
                             ReceivableInstallmentRepository installmentRepository,
                             ReceivablePaymentRepository paymentRepository,
                             CustomerRepository customerRepository,
                             CompanyRepository companyRepository) {
        this.repository = repository;
        this.installmentRepository = installmentRepository;
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
        if (request.issueDate() != null) {
            // issueDate não é persistido na entidade (não há coluna), mas
            // é usado como base para o cálculo dos vencimentos das parcelas
            // automáticas. Para manter o vencimento-base coerente quando o
            // usuário informar issueDate, recalculamos dueDate abaixo.
        }
        Receivable saved = repository.save(r);

        List<ReceivableInstallment> installments = buildInstallments(saved, request);
        // Valida coerência: soma das parcelas == value.
        validateInstallmentSum(installments, saved.getValue());
        installmentRepository.saveAll(installments);
        saved.setInstallmentsCount(installments.size());
        // Ajusta o vencimento-base para a primeira parcela (garante
        // coerência quando o usuário informou dueDate divergente da
        // primeira parcela explícita).
        saved.setDueDate(installments.get(0).getDueDate());
        Receivable persisted = repository.save(saved);
        return toResponseWithDetails(persisted);
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
                .map(this::toResponseWithDetails)
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
        return toResponseWithDetails(saved);
    }

    /**
     * Soft delete: marca a conta como CANCELADA. Cancela também as
     * parcelas ABERTO (sem pagamentos). Bloqueada para contas PAGO.
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
        // Cancela parcelas ainda ABERTO (sem pagamentos registrados).
        List<ReceivableInstallment> installments =
                installmentRepository.findByReceivableIdOrderByInstallmentNumberAsc(id);
        for (ReceivableInstallment inst : installments) {
            if (inst.getStatus() == ReceivableStatus.ABERTO) {
                BigDecimal paid = paymentRepository.sumAmountByInstallmentId(inst.getId());
                if (paid == null || paid.signum() <= 0) {
                    inst.setStatus(ReceivableStatus.CANCELADO);
                    installmentRepository.save(inst);
                }
            }
        }
    }

    /**
     * Reativa uma conta CANCELADA, voltando para ABERTO. Recalcula o
     * status com base nos pagamentos (volta para PAGO se já quitada).
     * Reativa também as parcelas CANCELADAS sem pagamentos.
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
        // Reativa parcelas CANCELADAS sem pagamentos.
        List<ReceivableInstallment> installments =
                installmentRepository.findByReceivableIdOrderByInstallmentNumberAsc(id);
        for (ReceivableInstallment inst : installments) {
            if (inst.getStatus() == ReceivableStatus.CANCELADO) {
                BigDecimal paid = paymentRepository.sumAmountByInstallmentId(inst.getId());
                if (paid == null || paid.signum() <= 0) {
                    inst.setStatus(ReceivableStatus.ABERTO);
                    installmentRepository.save(inst);
                }
            }
        }
        recomputeReceivableState(r);
        Receivable saved = repository.save(r);
        return toResponseWithDetails(saved);
    }

    // ---------------------------------------------------------------------
    // Parcelas — ação pós-criação "Gerar parcelas"
    // ---------------------------------------------------------------------

    /**
     * Gera parcelas programadas em uma conta a receber existente,
     * particionando o valor total. Ação disparada pelo botão "Gerar
     * parcelas" na UI.
     *
     * <p>Pré-requisitos (validados):</p>
     * <ul>
     *   <li>Conta ABERTO;</li>
     *   <li>{@code installmentsCount == 1} (não pode já ser parcelada);</li>
     *   <li>{@code paidAmount == 0} e nenhum pagamento registrado;</li>
     *   <li>Condição de pagamento informada (no request ou na conta) OU
     *       parcelas explícitas no request.</li>
     * </ul>
     */
    @Transactional
    public ReceivableResponse generateInstallments(Long receivableId,
                                                   GenerateInstallmentsRequest request) {
        Receivable r = repository.findById(receivableId)
                .orElseThrow(() -> new ReceivableNotFoundException(receivableId));
        if (r.getStatus() != ReceivableStatus.ABERTO) {
            throw ReceivableBusinessException.cannotGenerateInstallments(
                    "a conta deve estar ABERTO (status atual: " + r.getStatus() + ").");
        }
        if (r.getInstallmentsCount() > 1) {
            throw ReceivableBusinessException.cannotGenerateInstallments(
                    "a conta já possui " + r.getInstallmentsCount() + " parcelas.");
        }
        BigDecimal alreadyPaid = paymentRepository.sumAmountByReceivableId(receivableId);
        if (alreadyPaid != null && alreadyPaid.signum() > 0) {
            throw ReceivableBusinessException.cannotGenerateInstallments(
                    "a conta já possui pagamentos registrados.");
        }

        LocalDate baseDate = (request.baseDate() != null)
                ? request.baseDate()
                : r.getDueDate();

        List<ReceivableInstallment> newInstallments;
        if (request.installments() != null && !request.installments().isEmpty()) {
            // Parcelas explícitas informadas no request.
            newInstallments = buildExplicitInstallments(r, request.installments());
        } else if (request.paymentCondition() != null) {
            // Condição informada no request sobrescreve a da conta.
            newInstallments = buildInstallmentsFromCondition(r, request.paymentCondition(), baseDate);
        } else if (r.getPaymentCondition() != null) {
            // Usa a condição persistida na conta.
            newInstallments = buildInstallmentsFromCondition(r, r.getPaymentCondition(), baseDate);
        } else {
            throw ReceivableBusinessException.cannotGenerateInstallments(
                    "informe a condição de pagamento ou as parcelas explícitas.");
        }
        validateInstallmentSum(newInstallments, r.getValue());

        // Remove a parcela única original (sempre nº 1, ABERTO, sem
        // pagamentos — validado acima) e persiste as novas.
        List<ReceivableInstallment> existing =
                installmentRepository.findByReceivableIdOrderByInstallmentNumberAsc(receivableId);
        for (ReceivableInstallment old : existing) {
            installmentRepository.delete(old);
        }
        installmentRepository.flush();
        installmentRepository.saveAll(newInstallments);

        r.setInstallmentsCount(newInstallments.size());
        r.setDueDate(newInstallments.get(0).getDueDate());
        if (request.paymentCondition() != null) {
            r.setPaymentCondition(request.paymentCondition());
        }
        Receivable saved = repository.save(r);
        return toResponseWithDetails(saved);
    }

    /**
     * Preview de parcelas que seriam geradas a partir de uma condição de
     * pagamento e um valor total. Não persiste — apenas calcula.
     */
    public List<ReceivableInstallmentPreviewResponse> previewInstallments(
            PreviewInstallmentsRequest request) {
        LocalDate baseDate = (request.baseDate() != null) ? request.baseDate() : LocalDate.now();
        List<Integer> terms = PaymentConditionTerms.terms(request.paymentCondition());
        List<ReceivableInstallment> projected = buildInstallmentsFromTerms(
                request.value(), terms, baseDate, null);
        return projected.stream()
                .map(i -> new ReceivableInstallmentPreviewResponse(
                        i.getInstallmentNumber(), i.getAmount(), i.getDueDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivableInstallment> listInstallments(Long receivableId) {
        if (!repository.existsById(receivableId)) {
            throw new ReceivableNotFoundException(receivableId);
        }
        return installmentRepository.findByReceivableIdOrderByInstallmentNumberAsc(receivableId);
    }

    // ---------------------------------------------------------------------
    // Pagamentos (contra parcelas)
    // ---------------------------------------------------------------------

    /**
     * Registra um pagamento avulso contra uma parcela. Recalcula o
     * paidAmount e status da parcela, e em cascata o paidAmount e
     * status da conta pai.
     */
    @Transactional
    public ReceivableResponse registerPayment(Long receivableId,
                                              Long installmentId,
                                              ReceivablePaymentRequest request) {
        Receivable r = repository.findById(receivableId)
                .orElseThrow(() -> new ReceivableNotFoundException(receivableId));
        if (r.getStatus() != ReceivableStatus.ABERTO) {
            throw ReceivableBusinessException.installmentNotOpenForPayment();
        }
        ReceivableInstallment inst = installmentRepository
                .findByIdAndReceivableId(installmentId, receivableId)
                .orElseThrow(() -> new ReceivableInstallmentNotFoundException(installmentId, receivableId));
        if (inst.getStatus() != ReceivableStatus.ABERTO) {
            throw ReceivableBusinessException.installmentNotOpenForPayment();
        }

        BigDecimal currentPaid = (inst.getPaidAmount() != null) ? inst.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = inst.getAmount().subtract(currentPaid);
        if (request.amount().compareTo(balance) > 0) {
            throw ReceivableBusinessException.paymentExceedsInstallmentBalance(request.amount(), balance);
        }

        ReceivablePayment payment = ReceivableMapper.toPaymentEntity(receivableId, installmentId, request);
        paymentRepository.save(payment);

        recomputeInstallmentState(inst);
        recomputeReceivableState(r);

        installmentRepository.save(inst);
        Receivable saved = repository.save(r);
        return toResponseWithDetails(saved);
    }

    /**
     * Remove um pagamento e recalcula paidAmount/status da parcela e
     * da conta. A conta/parcela podem voltar para ABERTO se houver
     * saldo devedor novamente.
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
        Long installmentId = payment.getInstallmentId();
        paymentRepository.delete(payment);
        paymentRepository.flush();

        if (installmentId != null) {
            installmentRepository.findById(installmentId).ifPresent(this::recomputeInstallmentState);
        }
        recomputeReceivableState(r);

        Receivable saved = repository.save(r);
        return toResponseWithDetails(saved);
    }

    /**
     * Liquida todo o saldo devedor de uma parcela em um único pagamento,
     * transitando-a para PAGO. Rejeita parcelas que não estejam ABERTO
     * ou que já estejam quitadas.
     */
    @Transactional
    public ReceivableResponse settleInstallment(Long receivableId, Long installmentId) {
        Receivable r = repository.findById(receivableId)
                .orElseThrow(() -> new ReceivableNotFoundException(receivableId));
        if (r.getStatus() != ReceivableStatus.ABERTO) {
            throw ReceivableBusinessException.installmentNotOpenForPayment();
        }
        ReceivableInstallment inst = installmentRepository
                .findByIdAndReceivableId(installmentId, receivableId)
                .orElseThrow(() -> new ReceivableInstallmentNotFoundException(installmentId, receivableId));
        if (inst.getStatus() != ReceivableStatus.ABERTO) {
            throw ReceivableBusinessException.installmentNotOpenForPayment();
        }

        BigDecimal currentPaid = (inst.getPaidAmount() != null) ? inst.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = inst.getAmount().subtract(currentPaid);
        if (balance.signum() <= 0) {
            throw new ReceivableBusinessException(
                    "A parcela não possui saldo devedor a liquidar.");
        }

        ReceivablePayment payment = new ReceivablePayment();
        payment.setReceivableId(receivableId);
        payment.setInstallmentId(installmentId);
        payment.setAmount(balance);
        payment.setPaymentDate(LocalDate.now());
        payment.setNotes("Liquidação automática do saldo da parcela.");
        paymentRepository.save(payment);

        recomputeInstallmentState(inst);
        recomputeReceivableState(r);

        installmentRepository.save(inst);
        Receivable saved = repository.save(r);
        return toResponseWithDetails(saved);
    }

    /**
     * Baixa todas as parcelas ABERTO de uma conta em um único passo,
     * transitando a conta para PAGO. Rejeita contas que não estejam
     * ABERTO.
     */
    @Transactional
    public ReceivableResponse settle(Long receivableId) {
        Receivable r = repository.findById(receivableId)
                .orElseThrow(() -> new ReceivableNotFoundException(receivableId));
        if (r.getStatus() != ReceivableStatus.ABERTO) {
            throw ReceivableBusinessException.installmentNotOpenForPayment();
        }
        List<ReceivableInstallment> installments =
                installmentRepository.findByReceivableIdOrderByInstallmentNumberAsc(receivableId);
        LocalDate today = LocalDate.now();
        for (ReceivableInstallment inst : installments) {
            if (inst.getStatus() != ReceivableStatus.ABERTO) {
                continue;
            }
            BigDecimal currentPaid = (inst.getPaidAmount() != null) ? inst.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balance = inst.getAmount().subtract(currentPaid);
            if (balance.signum() <= 0) {
                continue;
            }
            ReceivablePayment payment = new ReceivablePayment();
            payment.setReceivableId(receivableId);
            payment.setInstallmentId(inst.getId());
            payment.setAmount(balance);
            payment.setPaymentDate(today);
            payment.setNotes("Liquidação automática do saldo da parcela.");
            paymentRepository.save(payment);
            recomputeInstallmentState(inst);
            installmentRepository.save(inst);
        }
        recomputeReceivableState(r);
        Receivable saved = repository.save(r);
        return toResponseWithDetails(saved);
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
     * para gerar as parcelas programadas.
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
        r.setDueDate(base); // ajustado abaixo para a 1ª parcela
        Receivable saved = repository.save(r);

        List<Integer> terms = PaymentConditionTerms.terms(order.getPaymentCondition());
        List<ReceivableInstallment> installments = buildInstallmentsFromTerms(total, terms, base, saved.getId());
        installmentRepository.saveAll(installments);
        saved.setInstallmentsCount(installments.size());
        saved.setDueDate(installments.get(0).getDueDate());
        return Optional.of(repository.save(saved));
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
        LocalDate base = LocalDate.now();
        r.setDueDate(base);
        Receivable saved = repository.save(r);

        List<Integer> terms = PaymentConditionTerms.terms(proposal.getPaymentCondition());
        List<ReceivableInstallment> installments = buildInstallmentsFromTerms(total, terms, base, saved.getId());
        installmentRepository.saveAll(installments);
        saved.setInstallmentsCount(installments.size());
        saved.setDueDate(installments.get(0).getDueDate());
        return Optional.of(repository.save(saved));
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
        // Contrato pode ter paymentCondition (V24) ou não — fallback 30 dias.
        PaymentCondition condition = contract.getPaymentCondition();
        r.setPaymentCondition(condition);
        LocalDate base = LocalDate.now();
        r.setDueDate(base);
        Receivable saved = repository.save(r);

        List<Integer> terms = (condition != null)
                ? PaymentConditionTerms.terms(condition)
                : PaymentConditionTerms.terms((PaymentCondition) null);
        List<ReceivableInstallment> installments = buildInstallmentsFromTerms(
                contract.getPrice(), terms, base, saved.getId());
        installmentRepository.saveAll(installments);
        saved.setInstallmentsCount(installments.size());
        saved.setDueDate(installments.get(0).getDueDate());
        return Optional.of(repository.save(saved));
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
    // Helpers — recálculo de estado
    // ---------------------------------------------------------------------

    /**
     * Recalcula paidAmount, status e paymentDate da parcela a partir
     * dos pagamentos vinculados. Não altera CANCELADO.
     */
    private void recomputeInstallmentState(ReceivableInstallment inst) {
        if (inst.getStatus() == ReceivableStatus.CANCELADO) {
            return;
        }
        BigDecimal sum = paymentRepository.sumAmountByInstallmentId(inst.getId());
        inst.setPaidAmount(sum != null ? sum : BigDecimal.ZERO);
        if (inst.getPaidAmount().compareTo(inst.getAmount()) >= 0) {
            inst.setStatus(ReceivableStatus.PAGO);
        } else {
            inst.setStatus(ReceivableStatus.ABERTO);
        }
        // paymentDate da parcela = data do pagamento mais recente.
        List<ReceivablePayment> payments =
                paymentRepository.findByInstallmentIdOrderByPaymentDateAsc(inst.getId());
        if (payments.isEmpty()) {
            inst.setPaymentDate(null);
        } else {
            inst.setPaymentDate(payments.get(payments.size() - 1).getPaymentDate());
        }
    }

    /**
     * Recalcula paidAmount, status e paymentDate da conta a partir das
     * parcelas. Não altera CANCELADO. A conta transita para PAGO
     * apenas quando <b>todas</b> as parcelas estão PAGO (ou seja, não
     * há parcelas ABERTO).
     */
    private void recomputeReceivableState(Receivable r) {
        if (r.getStatus() == ReceivableStatus.CANCELADO) {
            return;
        }
        BigDecimal sum = paymentRepository.sumAmountByReceivableId(r.getId());
        r.setPaidAmount(sum != null ? sum : BigDecimal.ZERO);
        List<ReceivableInstallment> installments =
                installmentRepository.findByReceivableIdOrderByInstallmentNumberAsc(r.getId());
        boolean anyOpen = installments.stream()
                .anyMatch(i -> i.getStatus() == ReceivableStatus.ABERTO);
        r.setStatus(anyOpen ? ReceivableStatus.ABERTO : ReceivableStatus.PAGO);
        // paymentDate da conta = data do pagamento mais recente entre
        // todas as parcelas.
        LocalDate latest = installments.stream()
                .map(ReceivableInstallment::getPaymentDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        r.setPaymentDate(latest);
    }

    // ---------------------------------------------------------------------
    // Helpers — geração de parcelas
    // ---------------------------------------------------------------------

    /**
     * Constrói as parcelas programadas conforme o request de criação:
     * <ul>
     *   <li>Se {@code installments} explícitas informadas → usa-as
     *       diretamente;</li>
     *   <li>Se {@code paymentCondition} informada e sem parcelas
     *       explícitas → gera parcelas automáticas a partir dos prazos
     *       da condição (ex.: PARCELAS_30_60_90 → 3 parcelas com
     *       vencimentos base+30, base+60, base+90), distribuindo o
     *       valor igualmente com residual na última;</li>
     *   <li>Caso contrário → 1 parcela (à vista) com valor total e
     *       vencimento-base.</li>
     * </ul>
     */
    private List<ReceivableInstallment> buildInstallments(Receivable receivable,
                                                          ReceivableCreateRequest request) {
        List<ReceivableInstallmentRequest> raw = request.installments();
        if (raw != null && !raw.isEmpty()) {
            return buildExplicitInstallments(receivable, raw);
        }
        if (request.paymentCondition() != null) {
            List<Integer> terms = PaymentConditionTerms.terms(request.paymentCondition());
            if (terms.size() > 1) {
                LocalDate base = (request.issueDate() != null) ? request.issueDate() : LocalDate.now();
                return buildInstallmentsFromTerms(receivable.getValue(), terms, base, receivable.getId());
            }
        }
        return ReceivableMapper.toInstallments(receivable.getId(), request);
    }

    /**
     * Materializa parcelas explícitas informadas no request (criação ou
     * geração pós-criação). Preserva valores e vencimentos informados.
     */
    private List<ReceivableInstallment> buildExplicitInstallments(Receivable receivable,
                                                                  List<ReceivableInstallmentRequest> raw) {
        List<ReceivableInstallment> result = new ArrayList<>(raw.size());
        int n = 1;
        for (ReceivableInstallmentRequest r : raw) {
            ReceivableInstallment inst = new ReceivableInstallment();
            inst.setReceivableId(receivable.getId());
            inst.setInstallmentNumber(n++);
            inst.setAmount(r.amount());
            inst.setDueDate(r.dueDate());
            inst.setPaidAmount(BigDecimal.ZERO);
            inst.setStatus(ReceivableStatus.ABERTO);
            result.add(inst);
        }
        return result;
    }

    /**
     * Gera parcelas a partir de uma condição de pagamento, usando
     * {@link PaymentConditionTerms#terms(PaymentCondition)} para obter
     * os prazos (em dias). Distribui o valor total igualmente, com o
     * residual absorvido pela última parcela.
     */
    private List<ReceivableInstallment> buildInstallmentsFromCondition(Receivable receivable,
                                                                       PaymentCondition condition,
                                                                       LocalDate baseDate) {
        List<Integer> terms = PaymentConditionTerms.terms(condition);
        return buildInstallmentsFromTerms(receivable.getValue(), terms, baseDate, receivable.getId());
    }

    /**
     * Gera parcelas a partir de uma lista de prazos (em dias),
     * distribuindo o valor total igualmente, com o residual absorvido
     * pela última parcela para que a soma bata exatamente com o valor.
     */
    private List<ReceivableInstallment> buildInstallmentsFromTerms(BigDecimal total,
                                                                   List<Integer> terms,
                                                                   LocalDate baseDate,
                                                                   Long receivableId) {
        int n = terms.size();
        if (n <= 0) {
            n = 1;
            terms = List.of(PaymentConditionTerms.DEFAULT_DAYS);
        }
        BigDecimal baseShare = total.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.HALF_UP);
        BigDecimal accumulated = BigDecimal.ZERO;
        List<ReceivableInstallment> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ReceivableInstallment inst = new ReceivableInstallment();
            inst.setReceivableId(receivableId);
            inst.setInstallmentNumber(i + 1);
            // Última parcela absorve o residual de arredondamento.
            if (i == n - 1) {
                inst.setAmount(total.subtract(accumulated));
            } else {
                inst.setAmount(baseShare);
                accumulated = accumulated.add(baseShare);
            }
            inst.setDueDate(baseDate.plusDays(terms.get(i)));
            inst.setPaidAmount(BigDecimal.ZERO);
            inst.setStatus(ReceivableStatus.ABERTO);
            result.add(inst);
        }
        return result;
    }

    /**
     * Valida que a soma dos valores das parcelas bate com o valor total
     * da conta. Tolerância de 1 centavo para diferenças de arredondamento
     * quando o usuário informou parcelas explícitas (não esperado, mas
     * possível em entradas manuais).
     */
    private void validateInstallmentSum(List<ReceivableInstallment> installments, BigDecimal total) {
        BigDecimal sum = installments.stream()
                .map(ReceivableInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diff = sum.subtract(total).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            throw new ReceivableBusinessException(
                    "A soma das parcelas (" + sum + ") não bate com o valor total da conta ("
                            + total + "). Diferença: " + diff + ".");
        }
    }

    // ---------------------------------------------------------------------
    // Helpers — validação e resolução de cliente
    // ---------------------------------------------------------------------

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

    private ReceivableResponse toResponseWithDetails(Receivable r) {
        List<ReceivableInstallment> installments =
                installmentRepository.findByReceivableIdOrderByInstallmentNumberAsc(r.getId());
        List<ReceivablePayment> payments =
                paymentRepository.findByReceivableIdOrderByPaymentDateAsc(r.getId());
        ClientResolved client = resolveClient(r);
        return ReceivableMapper.toResponse(r, client.name(), client.code(), installments, payments);
    }

    private record ClientResolved(String name, String code) {
        static final ClientResolved EMPTY = new ClientResolved(null, null);
    }
}