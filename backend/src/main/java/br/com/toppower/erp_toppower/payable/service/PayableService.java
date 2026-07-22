package br.com.toppower.erp_toppower.payable.service;

import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableCreateRequest;
import br.com.toppower.erp_toppower.payable.dto.PayableInstallmentRequest;
import br.com.toppower.erp_toppower.payable.dto.PayablePaymentRequest;
import br.com.toppower.erp_toppower.payable.dto.PayableResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableSummaryResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableUpdateRequest;
import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.entity.PayableInstallment;
import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import br.com.toppower.erp_toppower.payable.exception.InvalidPayableSupplierException;
import br.com.toppower.erp_toppower.payable.exception.PayableBusinessException;
import br.com.toppower.erp_toppower.payable.exception.PayableInstallmentNotFoundException;
import br.com.toppower.erp_toppower.payable.exception.PayableNotFoundException;
import br.com.toppower.erp_toppower.payable.exception.PayablePaymentNotFoundException;
import br.com.toppower.erp_toppower.payable.mapper.PayableMapper;
import br.com.toppower.erp_toppower.payable.repository.PayableInstallmentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayablePaymentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayableRepository;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Regras de negócio do módulo de contas a pagar.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>CRUD manual (create, search, getById, update, cancelar, reativar);</li>
 *   <li>Geração automática de parcelas programadas a partir da condição
 *       de pagamento (quando as parcelas explícitas não são informadas)
 *       ou uso das parcelas explícitas informadas pelo usuário;</li>
 *   <li>Registro e remoção de pagamentos avulsos contra parcelas, com
 *       recálculo automático do {@code paidAmount} da parcela, do
 *       {@code paidAmount} da conta e dos status (parcela → PAGO quando
 *       quitada; conta → PAGO quando todas as parcelas quitadas);</li>
 *   <li>Baixa total de uma parcela ou de todas as parcelas abertas;</li>
 *   <li>Geração automática de conta a pagar a partir de um boleto
 *       vinculado a um fornecedor.</li>
 * </ul>
 */
@Service
public class PayableService {

    private static final Logger log = LoggerFactory.getLogger(PayableService.class);
    private static final int MIN_SEARCH_QUERY_LENGTH = 2;
    private static final int SCALE = 2;

    private final PayableRepository repository;
    private final PayableInstallmentRepository installmentRepository;
    private final PayablePaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final PayablePaymentAttachmentService paymentAttachmentService;

    public PayableService(PayableRepository repository,
                          PayableInstallmentRepository installmentRepository,
                          PayablePaymentRepository paymentRepository,
                          SupplierRepository supplierRepository,
                          PayablePaymentAttachmentService paymentAttachmentService) {
        this.repository = repository;
        this.installmentRepository = installmentRepository;
        this.paymentRepository = paymentRepository;
        this.supplierRepository = supplierRepository;
        this.paymentAttachmentService = paymentAttachmentService;
    }

    // ---------------------------------------------------------------------
    // Create (manual)
    // ---------------------------------------------------------------------

    @Transactional
    public PayableResponse create(PayableCreateRequest request) {
        validateSupplier(request.supplierId());
        Payable p = PayableMapper.toEntity(request);
        Payable saved = repository.save(p);

        List<PayableInstallment> installments = buildInstallments(saved, request);
        // Valida coerência: soma das parcelas == value.
        validateInstallmentSum(installments, saved.getValue());
        installmentRepository.saveAll(installments);
        saved.setInstallmentsCount(installments.size());
        // Ajusta o vencimento-base para a primeira parcela (garante
        // coerência quando o usuário informou dueDate divergente da
        // primeira parcela explícita).
        saved.setDueDate(installments.get(0).getDueDate());
        Payable persisted = repository.save(saved);
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
    public PagedResponse<PayableSummaryResponse> search(PayableStatus status,
                                                         PayableSource sourceType,
                                                         Long supplierId,
                                                         LocalDate dueFrom,
                                                         LocalDate dueTo,
                                                         String query,
                                                         Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Specification<Payable> spec = (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (sourceType != null) {
                predicates.add(cb.equal(root.get("sourceType"), sourceType));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplierId"), supplierId));
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
                        cb.like(cb.lower(root.get("purchaseInvoiceNumber")),
                                "%" + trimmed.toLowerCase() + "%")
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Payable> page = repository.findAll(spec, pageable);
        Page<PayableSummaryResponse> mapped = page.map(p -> {
            SupplierResolved s = resolveSupplier(p);
            return PayableMapper.toSummary(p, s.name(), s.taxId());
        });
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PayableResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponseWithDetails)
                .orElseThrow(() -> new PayableNotFoundException(id));
    }

    // ---------------------------------------------------------------------
    // Update / Cancel / Activate
    // ---------------------------------------------------------------------

    @Transactional
    public PayableResponse update(Long id, PayableUpdateRequest request) {
        Payable p = repository.findById(id)
                .orElseThrow(() -> new PayableNotFoundException(id));
        if (p.getStatus() == PayableStatus.PAGO) {
            throw PayableBusinessException.cannotModifyPaid();
        }
        if (p.getStatus() == PayableStatus.CANCELADO) {
            throw new PayableBusinessException(
                    "Conta CANCELADA não pode ser alterada. Reative a conta antes de editar.");
        }
        PayableMapper.applyUpdate(p, request);
        Payable saved = repository.save(p);
        return toResponseWithDetails(saved);
    }

    /**
     * Soft delete: marca a conta como CANCELADA. Cancela também as
     * parcelas ABERTO (sem pagamentos). Bloqueada para contas PAGO.
     */
    @Transactional
    public void cancel(Long id) {
        Payable p = repository.findById(id)
                .orElseThrow(() -> new PayableNotFoundException(id));
        if (p.getStatus() == PayableStatus.PAGO) {
            throw PayableBusinessException.cannotModifyPaid();
        }
        p.setStatus(PayableStatus.CANCELADO);
        repository.save(p);
        // Cancela parcelas ainda ABERTO (sem pagamentos registrados).
        List<PayableInstallment> installments =
                installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(id);
        for (PayableInstallment inst : installments) {
            if (inst.getStatus() == PayableStatus.ABERTO) {
                BigDecimal paid = paymentRepository.sumAmountByInstallmentId(inst.getId());
                if (paid == null || paid.signum() <= 0) {
                    inst.setStatus(PayableStatus.CANCELADO);
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
    public PayableResponse activate(Long id) {
        Payable p = repository.findById(id)
                .orElseThrow(() -> new PayableNotFoundException(id));
        if (p.getStatus() != PayableStatus.CANCELADO) {
            throw new PayableBusinessException(
                    "Apenas contas CANCELADAS podem ser reativadas. Status atual: "
                            + p.getStatus());
        }
        // Reativa parcelas CANCELADAS sem pagamentos.
        List<PayableInstallment> installments =
                installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(id);
        for (PayableInstallment inst : installments) {
            if (inst.getStatus() == PayableStatus.CANCELADO) {
                BigDecimal paid = paymentRepository.sumAmountByInstallmentId(inst.getId());
                if (paid == null || paid.signum() <= 0) {
                    inst.setStatus(PayableStatus.ABERTO);
                    installmentRepository.save(inst);
                }
            }
        }
        recomputePayableState(p);
        Payable saved = repository.save(p);
        return toResponseWithDetails(saved);
    }

    // ---------------------------------------------------------------------
    // Pagamentos
    // ---------------------------------------------------------------------

    /**
     * Registra um pagamento avulso contra uma parcela. Recalcula o
     * paidAmount e status da parcela, e em cascata o paidAmount e
     * status da conta pai.
     */
    @Transactional
    public PayableResponse registerPayment(Long payableId,
                                            Long installmentId,
                                            PayablePaymentRequest request) {
        Payable p = repository.findById(payableId)
                .orElseThrow(() -> new PayableNotFoundException(payableId));
        if (p.getStatus() != PayableStatus.ABERTO) {
            throw PayableBusinessException.installmentNotOpenForPayment();
        }
        PayableInstallment inst = installmentRepository
                .findByIdAndPayableId(installmentId, payableId)
                .orElseThrow(() -> new PayableInstallmentNotFoundException(installmentId, payableId));
        if (inst.getStatus() != PayableStatus.ABERTO) {
            throw PayableBusinessException.installmentNotOpenForPayment();
        }

        BigDecimal currentPaid = (inst.getPaidAmount() != null) ? inst.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = inst.getAmount().subtract(currentPaid);
        if (request.amount().compareTo(balance) > 0) {
            throw PayableBusinessException.paymentExceedsInstallmentBalance(request.amount(), balance);
        }

        PayablePayment payment = PayableMapper.toPaymentEntity(payableId, installmentId, request);
        paymentRepository.save(payment);

        recomputeInstallmentState(inst);
        recomputePayableState(p);

        installmentRepository.save(inst);
        Payable saved = repository.save(p);
        return toResponseWithDetails(saved);
    }

    /**
     * Remove um pagamento e recalcula paidAmount/status da parcela e
     * da conta. A conta/parcela podem voltar para ABERTO se houver
     * saldo devedor novamente.
     */
    @Transactional
    public PayableResponse removePayment(Long payableId, Long paymentId) {
        Payable p = repository.findById(payableId)
                .orElseThrow(() -> new PayableNotFoundException(payableId));
        if (p.getStatus() == PayableStatus.CANCELADO) {
            throw new PayableBusinessException(
                    "Conta CANCELADA não permite remover pagamentos.");
        }
        PayablePayment payment = paymentRepository.findByIdAndPayableId(paymentId, payableId)
                .orElseThrow(() -> new PayablePaymentNotFoundException(paymentId, payableId));
        Long installmentId = payment.getInstallmentId();
        paymentRepository.delete(payment);
        paymentRepository.flush();

        installmentRepository.findById(installmentId).ifPresent(this::recomputeInstallmentState);
        recomputePayableState(p);

        Payable saved = repository.save(p);
        return toResponseWithDetails(saved);
    }

    /**
     * Liquida todo o saldo devedor de uma parcela em um único pagamento,
     * transitando-a para PAGO. Rejeita parcelas que não estejam ABERTO
     * ou que já estejam quitadas.
     */
    @Transactional
    public PayableResponse settleInstallment(Long payableId, Long installmentId) {
        Payable p = repository.findById(payableId)
                .orElseThrow(() -> new PayableNotFoundException(payableId));
        if (p.getStatus() != PayableStatus.ABERTO) {
            throw PayableBusinessException.installmentNotOpenForPayment();
        }
        PayableInstallment inst = installmentRepository
                .findByIdAndPayableId(installmentId, payableId)
                .orElseThrow(() -> new PayableInstallmentNotFoundException(installmentId, payableId));
        if (inst.getStatus() != PayableStatus.ABERTO) {
            throw PayableBusinessException.installmentNotOpenForPayment();
        }

        BigDecimal currentPaid = (inst.getPaidAmount() != null) ? inst.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = inst.getAmount().subtract(currentPaid);
        if (balance.signum() <= 0) {
            throw new PayableBusinessException(
                    "A parcela não possui saldo devedor a liquidar.");
        }

        PayablePayment payment = new PayablePayment();
        payment.setPayableId(payableId);
        payment.setInstallmentId(installmentId);
        payment.setAmount(balance);
        payment.setPaymentDate(LocalDate.now());
        payment.setNotes("Liquidação automática do saldo da parcela.");
        paymentRepository.save(payment);

        recomputeInstallmentState(inst);
        recomputePayableState(p);

        installmentRepository.save(inst);
        Payable saved = repository.save(p);
        return toResponseWithDetails(saved);
    }

    /**
     * Baixa todas as parcelas ABERTO de uma conta em um único passo,
     * transitando a conta para PAGO. Rejeita contas que não estejam
     * ABERTO.
     */
    @Transactional
    public PayableResponse settle(Long payableId) {
        return settle(payableId, null);
    }

    /**
     * Baixa todas as parcelas ABERTO de uma conta em um único passo,
     * transitando a conta para PAGO. Aceita um comprovante de pagamento
     * opcional que é vinculado ao primeiro pagamento gerado.
     */
    @Transactional
    public PayableResponse settle(Long payableId, MultipartFile receipt) {
        Payable p = repository.findById(payableId)
                .orElseThrow(() -> new PayableNotFoundException(payableId));
        if (p.getStatus() != PayableStatus.ABERTO) {
            throw PayableBusinessException.installmentNotOpenForPayment();
        }
        List<PayableInstallment> installments =
                installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(payableId);
        LocalDate today = LocalDate.now();
        boolean firstPayment = true;
        for (PayableInstallment inst : installments) {
            if (inst.getStatus() != PayableStatus.ABERTO) {
                continue;
            }
            BigDecimal currentPaid = (inst.getPaidAmount() != null) ? inst.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balance = inst.getAmount().subtract(currentPaid);
            if (balance.signum() <= 0) {
                continue;
            }
            PayablePayment payment = new PayablePayment();
            payment.setPayableId(payableId);
            payment.setInstallmentId(inst.getId());
            payment.setAmount(balance);
            payment.setPaymentDate(today);
            payment.setNotes("Liquidação automática do saldo da parcela.");
            paymentRepository.save(payment);

            // Vincula o comprovante ao primeiro pagamento gerado.
            if (firstPayment && receipt != null && !receipt.isEmpty()) {
                paymentAttachmentService.save(payment.getId(), receipt);
                firstPayment = false;
            }

            recomputeInstallmentState(inst);
            installmentRepository.save(inst);
        }
        recomputePayableState(p);
        Payable saved = repository.save(p);
        return toResponseWithDetails(saved);
    }

    @Transactional(readOnly = true)
    public List<PayableInstallment> listInstallments(Long payableId) {
        if (!repository.existsById(payableId)) {
            throw new PayableNotFoundException(payableId);
        }
        return installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(payableId);
    }

    @Transactional(readOnly = true)
    public List<PayablePayment> listPayments(Long payableId) {
        if (!repository.existsById(payableId)) {
            throw new PayableNotFoundException(payableId);
        }
        return paymentRepository.findByPayableIdOrderByPaymentDateAsc(payableId);
    }

    // ---------------------------------------------------------------------
    // Integrações — geração automática a partir de boleto
    // ---------------------------------------------------------------------

    /**
     * Gera uma conta a pagar a partir de um boleto vinculado a um
     * fornecedor. Idempotente: se já existe conta ativa para o
     * {@code boletoId}, retorna a existente. Cria uma única parcela
     * (à vista) com o valor e vencimento do boleto.
     *
     * <p>O boleto deve ter {@code supplierId} não nulo; caso contrário,
     * lança {@link PayableBusinessException#boletoWithoutSupplier}.</p>
     */
    @Transactional
    public Optional<Payable> generateFromBoleto(Boleto boleto) {
        if (boleto == null || boleto.getValue() == null || boleto.getValue().signum() <= 0) {
            return Optional.empty();
        }
        if (boleto.getSupplierId() == null) {
            // Boleto sem fornecedor não gera conta a pagar.
            return Optional.empty();
        }
        Optional<Payable> existing = repository.findActiveByBoletoId(boleto.getId());
        if (existing.isPresent()) {
            log.warn("Conta a pagar já existe para o boleto {}. Pulando geração.", boleto.getId());
            return existing;
        }

        Payable p = new Payable();
        p.setDescription("Boleto " + (boleto.getDescription() != null ? boleto.getDescription() : boleto.getId()));
        p.setValue(boleto.getValue());
        p.setIssueDate(LocalDate.now());
        p.setDueDate(boleto.getDueDate());
        p.setSupplierId(boleto.getSupplierId());
        p.setSourceType(PayableSource.BOLETO);
        p.setBoletoId(boleto.getId());
        Payable saved = repository.save(p);

        PayableInstallment inst = new PayableInstallment();
        inst.setPayableId(saved.getId());
        inst.setInstallmentNumber(1);
        inst.setAmount(boleto.getValue());
        inst.setDueDate(boleto.getDueDate());
        inst.setPaidAmount(BigDecimal.ZERO);
        inst.setStatus(PayableStatus.ABERTO);
        installmentRepository.save(inst);

        saved.setInstallmentsCount(1);
        repository.save(saved);
        return Optional.of(saved);
    }

    // ---------------------------------------------------------------------
    // Helpers — recálculo de estado
    // ---------------------------------------------------------------------

    /**
     * Recalcula paidAmount, status e paymentDate da parcela a partir
     * dos pagamentos vinculados. Não altera CANCELADO.
     */
    private void recomputeInstallmentState(PayableInstallment inst) {
        if (inst.getStatus() == PayableStatus.CANCELADO) {
            return;
        }
        BigDecimal sum = paymentRepository.sumAmountByInstallmentId(inst.getId());
        inst.setPaidAmount(sum != null ? sum : BigDecimal.ZERO);
        if (inst.getPaidAmount().compareTo(inst.getAmount()) >= 0) {
            inst.setStatus(PayableStatus.PAGO);
        } else {
            inst.setStatus(PayableStatus.ABERTO);
        }
        // paymentDate da parcela = data do pagamento mais recente.
        List<PayablePayment> payments =
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
    private void recomputePayableState(Payable p) {
        if (p.getStatus() == PayableStatus.CANCELADO) {
            return;
        }
        BigDecimal sum = paymentRepository.sumAmountByPayableId(p.getId());
        p.setPaidAmount(sum != null ? sum : BigDecimal.ZERO);
        List<PayableInstallment> installments =
                installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(p.getId());
        boolean anyOpen = installments.stream()
                .anyMatch(i -> i.getStatus() == PayableStatus.ABERTO);
        p.setStatus(anyOpen ? PayableStatus.ABERTO : PayableStatus.PAGO);
        // paymentDate da conta = data do pagamento mais recente entre
        // todas as parcelas.
        LocalDate latest = installments.stream()
                .map(PayableInstallment::getPaymentDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        p.setPaymentDate(latest);
    }

    // ---------------------------------------------------------------------
    // Helpers — geração de parcelas
    // ---------------------------------------------------------------------

    /**
     * Constrói as parcelas programadas conforme o request:
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
    private List<PayableInstallment> buildInstallments(Payable payable,
                                                       PayableCreateRequest request) {
        List<PayableInstallmentRequest> raw = request.installments();
        if (raw != null && !raw.isEmpty()) {
            return PayableMapper.toInstallments(payable.getId(), request);
        }
        // Sem parcelas explícitas.
        if (request.paymentCondition() != null) {
            List<Integer> terms = PaymentConditionTerms.terms(request.paymentCondition());
            if (terms.size() > 1) {
                return buildInstallmentsFromTerms(payable, terms);
            }
        }
        return PayableMapper.toInstallments(payable.getId(), request);
    }

    /**
     * Gera parcelas a partir de uma lista de prazos (em dias),
     * distribuindo o valor total igualmente, com o residual absorvido
     * pela última parcela para que a soma bata exatamente com o valor.
     */
    private List<PayableInstallment> buildInstallmentsFromTerms(Payable payable,
                                                                List<Integer> terms) {
        int n = terms.size();
        BigDecimal total = payable.getValue();
        BigDecimal baseShare = total.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.HALF_UP);
        BigDecimal accumulated = BigDecimal.ZERO;
        List<PayableInstallment> result = new ArrayList<>(n);
        LocalDate issueDate = payable.getIssueDate() != null ? payable.getIssueDate() : LocalDate.now();
        for (int i = 0; i < n; i++) {
            PayableInstallment inst = new PayableInstallment();
            inst.setPayableId(payable.getId());
            inst.setInstallmentNumber(i + 1);
            // Última parcela absorve o residual de arredondamento.
            if (i == n - 1) {
                inst.setAmount(total.subtract(accumulated));
            } else {
                inst.setAmount(baseShare);
                accumulated = accumulated.add(baseShare);
            }
            inst.setDueDate(issueDate.plusDays(terms.get(i)));
            inst.setPaidAmount(BigDecimal.ZERO);
            inst.setStatus(PayableStatus.ABERTO);
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
    private void validateInstallmentSum(List<PayableInstallment> installments, BigDecimal total) {
        BigDecimal sum = installments.stream()
                .map(PayableInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diff = sum.subtract(total).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            throw new PayableBusinessException(
                    "A soma das parcelas (" + sum + ") não bate com o valor total da conta ("
                            + total + "). Diferença: " + diff + ".");
        }
    }

    // ---------------------------------------------------------------------
    // Helpers — validação e resolução de fornecedor
    // ---------------------------------------------------------------------

    private void validateSupplier(Long supplierId) {
        if (supplierId == null) {
            throw InvalidPayableSupplierException.nullSupplier();
        }
        if (!supplierRepository.existsById(supplierId)) {
            throw InvalidPayableSupplierException.notFound(supplierId);
        }
    }

    /**
     * Resolve nome e CNPJ do fornecedor para exibição. Nome preferido:
     * {@code tradeName} se presente, senão {@code legalName}.
     */
    private SupplierResolved resolveSupplier(Payable p) {
        if (p.getSupplierId() == null) {
            return SupplierResolved.EMPTY;
        }
        return supplierRepository.findById(p.getSupplierId())
                .map(s -> new SupplierResolved(
                        (s.getTradeName() != null && !s.getTradeName().isBlank())
                                ? s.getTradeName()
                                : s.getLegalName(),
                        s.getTaxId()))
                .orElse(SupplierResolved.EMPTY);
    }

    private PayableResponse toResponseWithDetails(Payable p) {
        List<PayableInstallment> installments =
                installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(p.getId());
        List<PayablePayment> payments =
                paymentRepository.findByPayableIdOrderByPaymentDateAsc(p.getId());
        SupplierResolved s = resolveSupplier(p);
        return PayableMapper.toResponse(p, s.name(), s.taxId(), installments, payments);
    }

    private record SupplierResolved(String name, String taxId) {
        static final SupplierResolved EMPTY = new SupplierResolved(null, null);
    }
}