package br.com.toppower.erp_toppower.payable.report.service;

import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.entity.PayableInstallment;
import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import br.com.toppower.erp_toppower.payable.report.dto.PayableAgingReportResponse;
import br.com.toppower.erp_toppower.payable.report.dto.PayableAgingReportResponse.AgingBucket;
import br.com.toppower.erp_toppower.payable.report.dto.PayableAgingReportResponse.AgingBySupplier;
import br.com.toppower.erp_toppower.payable.report.dto.PayableFlowReportResponse;
import br.com.toppower.erp_toppower.payable.report.dto.PayableFlowReportResponse.FlowByPeriod;
import br.com.toppower.erp_toppower.payable.report.dto.PayableFlowReportResponse.FlowBySupplier;
import br.com.toppower.erp_toppower.payable.report.dto.PayableSupplierPositionReportResponse;
import br.com.toppower.erp_toppower.payable.report.dto.PayableSupplierPositionReportResponse.SupplierPosition;
import br.com.toppower.erp_toppower.payable.repository.PayableInstallmentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayablePaymentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayableRepository;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Serviço de relatórios de contas a pagar. Agrega em memória os dados
 * das {@link Payable}, {@link PayableInstallment} e
 * {@link PayablePayment} filtrados por fornecedor/origem/período,
 * reaproveitando o filtro baseado em {@link Specification} do módulo
 * de contas a pagar.
 *
 * <p>O isolamento por organização é automático via {@code organizationFilter}
 * (a entidade {@link Payable} é {@code OrganizationScopedEntity}); todas as
 * consultas aqui usam JPQL/Criteria, então nenhum tratamento manual é
 * necessário. As parcelas e pagamentos não são organization-scoped, mas
 * são sempre acessados via a conta pai (escopada).</p>
 *
 * <p><b>Aging por parcela</b>: ao contrário do contas a receber (onde o
 * aging é por conta), aqui o aging é calculado por parcela, pois uma
 * conta com parcelamento 30/60/90 tem vencimentos distintos em cada
 * parcela. O saldo devedor de cada parcela é
 * {@code installment.amount - installment.paidAmount}.</p>
 */
@Service
public class PayableReportService {

    private final PayableRepository repository;
    private final PayableInstallmentRepository installmentRepository;
    private final PayablePaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;

    public PayableReportService(PayableRepository repository,
                                PayableInstallmentRepository installmentRepository,
                                PayablePaymentRepository paymentRepository,
                                SupplierRepository supplierRepository) {
        this.repository = repository;
        this.installmentRepository = installmentRepository;
        this.paymentRepository = paymentRepository;
        this.supplierRepository = supplierRepository;
    }

    // ---------------------------------------------------------------------
    // Aging
    // ---------------------------------------------------------------------

    /**
     * Relatório aging: parcelas em ABERTO com saldo devedor, totalizadas
     * por faixa de atraso (0–30, 31–60, 61–90, 90+) e por fornecedor.
     * O aging é calculado pelo vencimento da parcela, não da conta pai.
     */
    @Transactional(readOnly = true)
    public PayableAgingReportResponse aging(PayableSource sourceType,
                                             Long supplierId,
                                             LocalDate dueTo) {
        LocalDate referenceDate = (dueTo != null) ? dueTo : LocalDate.now();
        // Carrega as contas a pagar elegíveis (status != CANCELADO, e
        // filtros opcionais de origem/fornecedor). As parcelas ABERTO
        // dessas contas serão agregadas.
        Specification<Payable> parentSpec = baseSpec(null, sourceType, supplierId, null, null)
                .and((root, q, cb) -> cb.notEqual(root.get("status"), PayableStatus.CANCELADO));
        Map<Long, Payable> parentById = new HashMap<>();
        for (Payable p : repository.findAll(parentSpec)) {
            parentById.put(p.getId(), p);
        }

        AgingTotals total = new AgingTotals();
        Map<Long, AgingSupplierAcc> bySupplier = new HashMap<>();
        for (Payable p : parentById.values()) {
            List<PayableInstallment> installments =
                    installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(p.getId());
            for (PayableInstallment inst : installments) {
                if (inst.getStatus() != PayableStatus.ABERTO) {
                    continue;
                }
                BigDecimal balance = installmentBalance(inst);
                if (balance.signum() <= 0) {
                    continue; // já quitada (marginal)
                }
                long days = daysBetween(inst.getDueDate(), referenceDate);
                int bucket = bucketIndex(days);
                total.add(bucket, balance);
                Long key = p.getSupplierId();
                AgingSupplierAcc acc = bySupplier.computeIfAbsent(key,
                        k -> new AgingSupplierAcc(key, resolveSupplier(p)));
                acc.add(bucket, balance);
            }
        }

        List<AgingBySupplier> suppliers = bySupplier.values().stream()
                .sorted(Comparator.comparing(AgingSupplierAcc::getTotalBalance).reversed())
                .map(AgingSupplierAcc::toDto)
                .toList();

        return new PayableAgingReportResponse(
                referenceDate,
                total.totalBalance,
                total.totalCount,
                total.bucket(0),
                total.bucket(1),
                total.bucket(2),
                total.bucket(3),
                suppliers);
    }

    // ---------------------------------------------------------------------
    // Flow (pagamentos)
    // ---------------------------------------------------------------------

    /**
     * Relatório de pagamentos num período, agrupados por granularidade
     * (dia/semana/mês) e por fornecedor.
     */
    @Transactional(readOnly = true)
    public PayableFlowReportResponse flow(PayableSource sourceType,
                                          Long supplierId,
                                          LocalDate from,
                                          LocalDate to,
                                          Granularity granularity) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "As datas 'from' e 'to' são obrigatórias para o relatório de fluxo.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' não pode ser posterior a 'to'.");
        }
        Granularity gran = (granularity != null) ? granularity : Granularity.MONTH;

        // Filtra payables pai pelo fornecedor/origem (para resolver o
        // fornecedor de cada pagamento). Carrega todos os pagamentos do
        // período e filtra em memória pelos payables elegíveis.
        Specification<Payable> parentSpec = baseSpec(null, sourceType, supplierId, null, null);
        Map<Long, Payable> parentById = new HashMap<>();
        for (Payable p : repository.findAll(parentSpec)) {
            parentById.put(p.getId(), p);
        }

        List<PayablePayment> payments = paymentRepository.findByPaymentDateBetween(from, to);
        BigDecimal totalPaid = BigDecimal.ZERO;
        long paymentCount = 0L;
        Map<PeriodKey, FlowAcc> byPeriod = new TreeMap<>();
        Map<Long, FlowAcc> bySupplier = new HashMap<>();
        for (PayablePayment pay : payments) {
            Payable parent = parentById.get(pay.getPayableId());
            if (parent == null) {
                continue; // pagamento de conta fora do filtro
            }
            BigDecimal amount = pay.getAmount() != null ? pay.getAmount() : BigDecimal.ZERO;
            totalPaid = totalPaid.add(amount);
            paymentCount++;
            PeriodKey pk = periodKey(pay.getPaymentDate(), gran);
            byPeriod.computeIfAbsent(pk, k -> new FlowAcc()).add(amount);
            Long key = parent.getSupplierId();
            bySupplier.computeIfAbsent(key, k -> new FlowAcc(resolveSupplier(parent)))
                    .add(amount);
        }

        List<FlowByPeriod> periods = byPeriod.entrySet().stream()
                .map(e -> new FlowByPeriod(e.getKey().start, e.getKey().label,
                        e.getValue().total, e.getValue().count))
                .toList();
        List<FlowBySupplier> suppliers = bySupplier.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Long, FlowAcc> e) -> e.getValue().total).reversed())
                .map(e -> new FlowBySupplier(e.getKey(), e.getValue().supplierName,
                        e.getValue().total, e.getValue().count))
                .toList();

        return new PayableFlowReportResponse(
                from, to, gran.name(), totalPaid, paymentCount, periods, suppliers);
    }

    // ---------------------------------------------------------------------
    // Posição por fornecedor
    // ---------------------------------------------------------------------

    /**
     * Posição consolidada por fornecedor: total a pagar, total pago,
     * parcelas em aberto, parcelas em atraso e maior atraso em dias.
     */
    @Transactional(readOnly = true)
    public PayableSupplierPositionReportResponse supplierPosition(PayableSource sourceType,
                                                                   Long supplierId,
                                                                   LocalDate dueTo) {
        LocalDate referenceDate = (dueTo != null) ? dueTo : LocalDate.now();
        // Considera todas as não-CANCELADO para total pago.
        Specification<Payable> spec = baseSpec(null, sourceType, supplierId, null, null)
                .and((root, q, cb) -> cb.notEqual(root.get("status"), PayableStatus.CANCELADO));
        List<Payable> rows = repository.findAll(spec);

        Map<Long, SupplierAcc> bySupplier = new HashMap<>();
        for (Payable p : rows) {
            Long key = p.getSupplierId();
            SupplierAcc acc = bySupplier.computeIfAbsent(key, k -> new SupplierAcc(key, resolveSupplier(p)));
            BigDecimal paid = (p.getPaidAmount() != null) ? p.getPaidAmount() : BigDecimal.ZERO;
            acc.totalPaid = acc.totalPaid.add(paid);
            // Parcelas em aberto para total a pagar + atraso.
            List<PayableInstallment> installments =
                    installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(p.getId());
            for (PayableInstallment inst : installments) {
                if (inst.getStatus() != PayableStatus.ABERTO) {
                    continue;
                }
                BigDecimal balance = installmentBalance(inst);
                acc.totalToPay = acc.totalToPay.add(balance);
                acc.openCount++;
                long overdueDays = daysBetween(inst.getDueDate(), referenceDate);
                if (overdueDays > 0) {
                    acc.overdueCount++;
                    acc.maxOverdueDays = Math.max(acc.maxOverdueDays, overdueDays);
                }
            }
        }

        List<SupplierPosition> suppliers = bySupplier.values().stream()
                .sorted(Comparator.comparing(SupplierAcc::getTotalToPay).reversed())
                .map(SupplierAcc::toDto)
                .toList();
        return new PayableSupplierPositionReportResponse(referenceDate, suppliers);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Specification<Payable> baseSpec(PayableStatus status,
                                            PayableSource sourceType,
                                            Long supplierId,
                                            LocalDate dueFrom,
                                            LocalDate dueTo) {
        return (root, q, cb) -> {
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
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private BigDecimal installmentBalance(PayableInstallment inst) {
        BigDecimal paid = (inst.getPaidAmount() != null) ? inst.getPaidAmount() : BigDecimal.ZERO;
        return inst.getAmount().subtract(paid);
    }

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

    /**
     * Dias entre {@code dueDate} e {@code referenceDate}. Positivo quando
     * {@code dueDate} está no passado (em atraso); negativo quando ainda
     * não venceu.
     */
    private long daysBetween(LocalDate dueDate, LocalDate referenceDate) {
        if (dueDate == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(dueDate, referenceDate);
    }

    /**
     * Índice da faixa de aging: 0 = 0–30, 1 = 31–60, 2 = 61–90, 3 = 90+.
     * Contas ainda não vencidas (dias &lt; 0) caem na faixa 0–30.
     */
    private int bucketIndex(long days) {
        if (days < 0) {
            return 0;
        }
        if (days <= 30) {
            return 0;
        }
        if (days <= 60) {
            return 1;
        }
        if (days <= 90) {
            return 2;
        }
        return 3;
    }

    private PeriodKey periodKey(LocalDate date, Granularity gran) {
        Locale ptBr = new Locale("pt", "BR");
        return switch (gran) {
            case DAY -> new PeriodKey(date, date.format(java.time.format.DateTimeFormatter
                    .ofPattern("dd/MM/yyyy", ptBr)));
            case WEEK -> {
                WeekFields wf = WeekFields.of(ptBr);
                int week = date.get(wf.weekOfWeekBasedYear());
                int year = date.get(wf.weekBasedYear());
                LocalDate monday = date.with(wf.dayOfWeek(), 1);
                yield new PeriodKey(monday, "Sem " + String.format("%02d", week) + "/" + year);
            }
            case MONTH -> {
                YearMonth ym = YearMonth.from(date);
                yield new PeriodKey(ym.atDay(1), ym.format(java.time.format.DateTimeFormatter
                        .ofPattern("MM/yyyy", ptBr)));
            }
        };
    }

    // ---------------------------------------------------------------------
    // Tipos auxiliares internos
    // ---------------------------------------------------------------------

    private record SupplierResolved(String name, String taxId) {
        static final SupplierResolved EMPTY = new SupplierResolved(null, null);
    }

    /** Granularidade do agrupamento por período no relatório de fluxo. */
    public enum Granularity {
        DAY, WEEK, MONTH
    }

    private static final class AgingTotals {
        private final long[] counts = new long[4];
        private final BigDecimal[] balances = new BigDecimal[]{
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        private long totalCount = 0L;
        private BigDecimal totalBalance = BigDecimal.ZERO;

        void add(int bucket, BigDecimal amount) {
            counts[bucket]++;
            balances[bucket] = balances[bucket].add(amount);
            totalCount++;
            totalBalance = totalBalance.add(amount);
        }

        AgingBucket bucket(int i) {
            return new AgingBucket(counts[i], balances[i].setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static final class AgingSupplierAcc {
        private final long supplierId;
        private final SupplierResolved supplier;
        private final long[] counts = new long[4];
        private final BigDecimal[] balances = new BigDecimal[]{
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        private long count = 0L;
        private BigDecimal totalBalance = BigDecimal.ZERO;

        AgingSupplierAcc(long supplierId, SupplierResolved supplier) {
            this.supplierId = supplierId;
            this.supplier = supplier;
        }

        void add(int bucket, BigDecimal amount) {
            counts[bucket]++;
            balances[bucket] = balances[bucket].add(amount);
            count++;
            totalBalance = totalBalance.add(amount);
        }

        BigDecimal getTotalBalance() {
            return totalBalance;
        }

        AgingBySupplier toDto() {
            return new AgingBySupplier(
                    supplierId, supplier.name(), supplier.taxId(),
                    totalBalance.setScale(2, RoundingMode.HALF_UP), count,
                    bucket(0), bucket(1), bucket(2), bucket(3));
        }

        private AgingBucket bucket(int i) {
            return new AgingBucket(counts[i], balances[i].setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static final class FlowAcc {
        private BigDecimal total = BigDecimal.ZERO;
        private long count = 0L;
        private final String supplierName;

        FlowAcc() {
            this(null);
        }

        FlowAcc(SupplierResolved supplier) {
            this.supplierName = (supplier != null) ? supplier.name() : null;
        }

        void add(BigDecimal amount) {
            total = total.add(amount);
            count++;
        }
    }

    private record PeriodKey(LocalDate start, String label) implements Comparable<PeriodKey> {
        @Override
        public int compareTo(PeriodKey o) {
            return this.start.compareTo(o.start);
        }
    }

    private static final class SupplierAcc {
        private final long supplierId;
        private final SupplierResolved supplier;
        private BigDecimal totalToPay = BigDecimal.ZERO;
        private BigDecimal totalPaid = BigDecimal.ZERO;
        private long openCount = 0L;
        private long overdueCount = 0L;
        private long maxOverdueDays = 0L;

        SupplierAcc(long supplierId, SupplierResolved supplier) {
            this.supplierId = supplierId;
            this.supplier = supplier;
        }

        BigDecimal getTotalToPay() {
            return totalToPay;
        }

        SupplierPosition toDto() {
            return new SupplierPosition(
                    supplierId, supplier.name(), supplier.taxId(),
                    totalToPay.setScale(2, RoundingMode.HALF_UP),
                    totalPaid.setScale(2, RoundingMode.HALF_UP),
                    openCount, overdueCount, maxOverdueDays);
        }
    }
}