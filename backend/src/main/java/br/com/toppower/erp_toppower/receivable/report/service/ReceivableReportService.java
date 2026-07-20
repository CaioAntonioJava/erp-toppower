package br.com.toppower.erp_toppower.receivable.report.service;

import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.receivable.entity.Receivable;
import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableAgingReportResponse;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableAgingReportResponse.AgingBucket;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableAgingReportResponse.AgingByClient;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableClientPositionReportResponse;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableClientPositionReportResponse.ClientPosition;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableFlowReportResponse;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableFlowReportResponse.FlowByClient;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableFlowReportResponse.FlowByPeriod;
import br.com.toppower.erp_toppower.receivable.repository.ReceivablePaymentRepository;
import br.com.toppower.erp_toppower.receivable.repository.ReceivableRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
import java.util.stream.Collectors;

/**
 * Serviço de relatórios de contas a receber. Agrega em memória os dados
 * das {@link Receivable} e {@link ReceivablePayment} filtrados por
 * cliente/origem/período, reaproveitando o filtro baseado em
 * {@link Specification} do módulo de contas a receber.
 *
 * <p>O isolamento por organização é automático via {@code organizationFilter}
 * (a entidade é {@code OrganizationScopedEntity}); todas as consultas
 * aqui usam JPQL/Criteria, então nenhum tratamento manual é necessário.</p>
 */
@Service
public class ReceivableReportService {

    private final ReceivableRepository repository;
    private final ReceivablePaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;

    public ReceivableReportService(ReceivableRepository repository,
                                   ReceivablePaymentRepository paymentRepository,
                                   CustomerRepository customerRepository,
                                   CompanyRepository companyRepository) {
        this.repository = repository;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
    }

    // ---------------------------------------------------------------------
    // Aging
    // ---------------------------------------------------------------------

    /**
     * Relatório aging: contas em ABERTO com saldo devedor, totalizadas por
     * faixa de atraso (0–30, 31–60, 61–90, 90+) e por cliente.
     */
    @Transactional(readOnly = true)
    public ReceivableAgingReportResponse aging(ReceivableSource sourceType,
                                               Long clientId,
                                               LocalDate dueTo) {
        LocalDate referenceDate = (dueTo != null) ? dueTo : LocalDate.now();
        Specification<Receivable> spec = baseSpec(ReceivableStatus.ABERTO, sourceType, clientId, null, null);
        List<Receivable> rows = repository.findAll(spec);

        AgingTotals total = new AgingTotals();
        Map<Long, AgingClientAcc> byClient = new HashMap<>();
        for (Receivable r : rows) {
            BigDecimal balance = balanceOf(r);
            if (balance.signum() <= 0) {
                continue; // já quitada (marginal)
            }
            long days = daysBetween(r.getDueDate(), referenceDate);
            int bucket = bucketIndex(days);
            total.add(bucket, balance);
            Long key = clientKey(r);
            AgingClientAcc acc = byClient.computeIfAbsent(key, k -> new AgingClientAcc(key, resolveClient(r)));
            acc.add(bucket, balance);
        }

        List<AgingByClient> clients = byClient.values().stream()
                .sorted(Comparator.comparing(AgingClientAcc::getTotalBalance).reversed())
                .map(AgingClientAcc::toDto)
                .toList();

        return new ReceivableAgingReportResponse(
                referenceDate,
                total.totalBalance,
                total.totalCount,
                total.bucket(0),
                total.bucket(1),
                total.bucket(2),
                total.bucket(3),
                clients);
    }

    // ---------------------------------------------------------------------
    // Flow (recebimentos)
    // ---------------------------------------------------------------------

    /**
     * Relatório de recebimentos num período, agrupados por granularidade
     * (dia/semana/mês) e por cliente.
     */
    @Transactional(readOnly = true)
    public ReceivableFlowReportResponse flow(ReceivableSource sourceType,
                                             Long clientId,
                                             LocalDate from,
                                             LocalDate to,
                                             Granularity granularity) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("As datas 'from' e 'to' são obrigatórias para o relatório de fluxo.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' não pode ser posterior a 'to'.");
        }
        Granularity gran = (granularity != null) ? granularity : Granularity.MONTH;

        // Filtra receivables pai pelo cliente/origem (para resolver o cliente
        // de cada pagamento). Carrega todos os pagamentos do período e filtra
        // em memória pelos receivables elegíveis. Volume típico por período
        // é baixo, então evitar JOIN nativo simplifica o multi-tenancy.
        Specification<Receivable> parentSpec = baseSpec(null, sourceType, clientId, null, null);
        Map<Long, Receivable> parentById = new HashMap<>();
        for (Receivable r : repository.findAll(parentSpec)) {
            parentById.put(r.getId(), r);
        }

        List<ReceivablePayment> payments = paymentRepository.findByPaymentDateBetween(from, to);
        BigDecimal totalReceived = BigDecimal.ZERO;
        long paymentCount = 0L;
        Map<PeriodKey, FlowAcc> byPeriod = new TreeMap<>();
        Map<Long, FlowAcc> byClient = new HashMap<>();
        for (ReceivablePayment p : payments) {
            Receivable parent = parentById.get(p.getReceivableId());
            if (parent == null) {
                continue; // pagamento de conta fora do filtro (cliente/origem)
            }
            BigDecimal amount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            totalReceived = totalReceived.add(amount);
            paymentCount++;
            PeriodKey pk = periodKey(p.getPaymentDate(), gran);
            byPeriod.computeIfAbsent(pk, k -> new FlowAcc()).add(amount);
            Long key = clientKey(parent);
            byClient.computeIfAbsent(key, k -> new FlowAcc(resolveClient(parent))).add(amount);
        }

        List<FlowByPeriod> periods = byPeriod.entrySet().stream()
                .map(e -> new FlowByPeriod(e.getKey().start, e.getKey().label, e.getValue().total, e.getValue().count))
                .toList();
        List<FlowByClient> clients = byClient.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Long, FlowAcc> e) -> e.getValue().total).reversed())
                .map(e -> new FlowByClient(e.getKey(), e.getValue().clientName, e.getValue().total, e.getValue().count))
                .toList();

        return new ReceivableFlowReportResponse(
                from, to, gran.name(), totalReceived, paymentCount, periods, clients);
    }

    // ---------------------------------------------------------------------
    // Posição por cliente
    // ---------------------------------------------------------------------

    /**
     * Posição consolidada por cliente: total a receber, total recebido,
     * contas em aberto, contas em atraso e maior atraso em dias.
     */
    @Transactional(readOnly = true)
    public ReceivableClientPositionReportResponse clientPosition(ReceivableSource sourceType,
                                                                 Long clientId,
                                                                 LocalDate dueTo) {
        LocalDate referenceDate = (dueTo != null) ? dueTo : LocalDate.now();
        // Considera todas as não-CANCELADO (ABERTO + PAGO) para total recebido.
        Specification<Receivable> spec = baseSpec(null, sourceType, clientId, null, null)
                .and((root, q, cb) -> cb.notEqual(root.get("status"), ReceivableStatus.CANCELADO));
        List<Receivable> rows = repository.findAll(spec);

        Map<Long, ClientAcc> byClient = new HashMap<>();
        for (Receivable r : rows) {
            Long key = clientKey(r);
            ClientAcc acc = byClient.computeIfAbsent(key, k -> new ClientAcc(key, resolveClient(r)));
            BigDecimal paid = paidOf(r);
            acc.totalReceived = acc.totalReceived.add(paid);
            if (r.getStatus() == ReceivableStatus.ABERTO) {
                BigDecimal balance = balanceOf(r);
                acc.totalToReceive = acc.totalToReceive.add(balance);
                acc.openCount++;
                long overdueDays = daysBetween(r.getDueDate(), referenceDate);
                if (overdueDays > 0) {
                    acc.overdueCount++;
                    acc.maxOverdueDays = Math.max(acc.maxOverdueDays, overdueDays);
                }
            }
        }

        List<ClientPosition> clients = byClient.values().stream()
                .sorted(Comparator.comparing(ClientAcc::getTotalToReceive).reversed())
                .map(ClientAcc::toDto)
                .toList();
        return new ReceivableClientPositionReportResponse(referenceDate, clients);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Specification<Receivable> baseSpec(ReceivableStatus status,
                                               ReceivableSource sourceType,
                                               Long clientId,
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
            if (clientId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("customerId"), clientId),
                        cb.equal(root.get("companyId"), clientId)));
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

    private BigDecimal balanceOf(Receivable r) {
        BigDecimal paid = paidOf(r);
        return r.getValue().subtract(paid);
    }

    private BigDecimal paidOf(Receivable r) {
        return (r.getPaidAmount() != null) ? r.getPaidAmount() : BigDecimal.ZERO;
    }

    private Long clientKey(Receivable r) {
        return (r.getCustomerId() != null) ? r.getCustomerId() : r.getCompanyId();
    }

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

    private record ClientResolved(String name, String code) {
        static final ClientResolved EMPTY = new ClientResolved(null, null);
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
            return new AgingBucket(counts[i], balances[i].setScale(2, java.math.RoundingMode.HALF_UP));
        }
    }

    private static final class AgingClientAcc {
        private final long clientId;
        private final ClientResolved client;
        private final long[] counts = new long[4];
        private final BigDecimal[] balances = new BigDecimal[]{
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        private long count = 0L;
        private BigDecimal totalBalance = BigDecimal.ZERO;

        AgingClientAcc(long clientId, ClientResolved client) {
            this.clientId = clientId;
            this.client = client;
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

        AgingByClient toDto() {
            return new AgingByClient(
                    clientId, client.name(), client.code(),
                    totalBalance.setScale(2, java.math.RoundingMode.HALF_UP), count,
                    bucket(0), bucket(1), bucket(2), bucket(3));
        }

        private AgingBucket bucket(int i) {
            return new AgingBucket(counts[i], balances[i].setScale(2, java.math.RoundingMode.HALF_UP));
        }
    }

    private static final class FlowAcc {
        private BigDecimal total = BigDecimal.ZERO;
        private long count = 0L;
        private final String clientName;

        FlowAcc() {
            this(null);
        }

        FlowAcc(ClientResolved client) {
            this.clientName = (client != null) ? client.name() : null;
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

    private static final class ClientAcc {
        private final long clientId;
        private final ClientResolved client;
        private BigDecimal totalToReceive = BigDecimal.ZERO;
        private BigDecimal totalReceived = BigDecimal.ZERO;
        private long openCount = 0L;
        private long overdueCount = 0L;
        private long maxOverdueDays = 0L;

        ClientAcc(long clientId, ClientResolved client) {
            this.clientId = clientId;
            this.client = client;
        }

        BigDecimal getTotalToReceive() {
            return totalToReceive;
        }

        ClientPosition toDto() {
            return new ClientPosition(
                    clientId, client.name(), client.code(),
                    totalToReceive.setScale(2, java.math.RoundingMode.HALF_UP),
                    totalReceived.setScale(2, java.math.RoundingMode.HALF_UP),
                    openCount, overdueCount, maxOverdueDays);
        }
    }
}