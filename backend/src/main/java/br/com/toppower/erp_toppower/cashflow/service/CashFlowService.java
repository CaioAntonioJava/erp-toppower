package br.com.toppower.erp_toppower.cashflow.service;

import br.com.toppower.erp_toppower.cashflow.dto.CashFlowEntryResponse;
import br.com.toppower.erp_toppower.cashflow.dto.CashFlowSettingsRequest;
import br.com.toppower.erp_toppower.cashflow.dto.CashFlowSettingsResponse;
import br.com.toppower.erp_toppower.cashflow.dto.CashFlowSummaryResponse;
import br.com.toppower.erp_toppower.cashflow.entity.CashFlowSettings;
import br.com.toppower.erp_toppower.cashflow.enums.CashFlowClientType;
import br.com.toppower.erp_toppower.cashflow.enums.CashFlowDocumentSource;
import br.com.toppower.erp_toppower.cashflow.enums.CashFlowEntryType;
import br.com.toppower.erp_toppower.cashflow.repository.CashFlowSettingsRepository;
import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.entity.Company;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.customer.entity.Customer;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import br.com.toppower.erp_toppower.payable.repository.PayablePaymentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayableRepository;
import br.com.toppower.erp_toppower.receivable.entity.Receivable;
import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import br.com.toppower.erp_toppower.receivable.repository.ReceivablePaymentRepository;
import br.com.toppower.erp_toppower.receivable.repository.ReceivableRepository;
import br.com.toppower.erp_toppower.sales.quotation.dto.ClientSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.service.ClientSearchService;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de consolidação do fluxo de caixa: une os pagamentos
 * efetivados de contas a receber (entradas) e de contas a pagar (saídas)
 * em um ledger único, com saldo do caixa, totais por período e saldo
 * inicial configurável por organização.
 *
 * <p>O isolamento por organização é garantido pelas contas-pai
 * ({@link Receivable}/{@link Payable}, organization-scoped) carregadas
 * sempre escopadas pelo {@code organizationFilter}; os pagamentos em si
 * não são organization-scoped (mesma estratégia dos relatórios de contas
 * a receber/pagar). As agregações são feitas em memória — o volume
 * típico de pagamentos por período é baixo, e evitar JOIN nativo
 * simplifica o multi-tenancy (mesmo padrão de
 * {@code ReceivableReportService}).</p>
 */
@Service
public class CashFlowService {

    /**
     * Teto de data para consultas "até o presente": limite do tipo DATE
     * no MySQL, usado quando o período é aberto no fim.
     */
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final CashFlowSettingsRepository settingsRepository;
    private final ReceivableRepository receivableRepository;
    private final ReceivablePaymentRepository receivablePaymentRepository;
    private final PayableRepository payableRepository;
    private final PayablePaymentRepository payablePaymentRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final SupplierRepository supplierRepository;
    private final ClientSearchService clientSearchService;

    public CashFlowService(CashFlowSettingsRepository settingsRepository,
                           ReceivableRepository receivableRepository,
                           ReceivablePaymentRepository receivablePaymentRepository,
                           PayableRepository payableRepository,
                           PayablePaymentRepository payablePaymentRepository,
                           CustomerRepository customerRepository,
                           CompanyRepository companyRepository,
                           SupplierRepository supplierRepository,
                           ClientSearchService clientSearchService) {
        this.settingsRepository = settingsRepository;
        this.receivableRepository = receivableRepository;
        this.receivablePaymentRepository = receivablePaymentRepository;
        this.payableRepository = payableRepository;
        this.payablePaymentRepository = payablePaymentRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.supplierRepository = supplierRepository;
        this.clientSearchService = clientSearchService;
    }

    // ---------------------------------------------------------------------
    // Saldo inicial
    // ---------------------------------------------------------------------

    /**
     * Configuração do fluxo de caixa da organização corrente. Quando a
     * organização ainda não configurou saldo inicial, retorna o default
     * (R$ 0,00) — não lança 404, pois a configuração é opcional.
     */
    @Transactional(readOnly = true)
    public CashFlowSettingsResponse getSettings() {
        Long organizationId = OrganizationContext.get();
        if (organizationId == null) {
            return CashFlowSettingsResponse.empty();
        }
        return settingsRepository.findByOrganizationId(organizationId)
                .map(settings -> new CashFlowSettingsResponse(
                        scale(settings.getOpeningBalance()),
                        settings.getUpdatedAt(),
                        settings.getUpdatedBy()))
                .orElseGet(CashFlowSettingsResponse::empty);
    }

    /**
     * Cria ou atualiza (upsert) o saldo inicial da organização corrente.
     */
    @Transactional
    public CashFlowSettingsResponse updateSettings(CashFlowSettingsRequest request) {
        Long organizationId = OrganizationContext.require();
        CashFlowSettings settings = settingsRepository.findByOrganizationId(organizationId)
                .orElseGet(CashFlowSettings::new);
        settings.setOrganizationId(organizationId);
        settings.setOpeningBalance(request.openingBalance().setScale(2, RoundingMode.HALF_UP));
        CashFlowSettings saved = settingsRepository.save(settings);
        return new CashFlowSettingsResponse(
                scale(saved.getOpeningBalance()),
                saved.getUpdatedAt(),
                saved.getUpdatedBy());
    }

    // ---------------------------------------------------------------------
    // Resumo (totais + série por período)
    // ---------------------------------------------------------------------

    /**
     * Resumo consolidado do fluxo de caixa no período informado.
     *
     * <p>Semântica: {@code saldoAnterior} (e o {@code saldoAcumulado} da
     * série) são a <b>posição real do caixa</b> — consideram todos os
     * movimentos, independentemente dos filtros. Os totais de
     * entradas/saídas/resultado e a série por período respeitam os
     * filtros informados.</p>
     */
    @Transactional(readOnly = true)
    public CashFlowSummaryResponse summary(LocalDate from, LocalDate to,
                                           CashFlowAggregator.Granularity granularity,
                                           CashFlowEntryType type,
                                           CashFlowDocumentSource sourceType,
                                           CashFlowClientType clientType,
                                           Long clientId,
                                           String query) {
        validatePeriod(from, to);
        LocalDate effectiveTo = (to != null) ? to : MAX_DATE;
        BigDecimal openingBalance = getOpeningBalance();
        List<LedgerEntry> ledger = buildLedger(effectiveTo, openingBalance);

        BigDecimal saldoAnterior = saldoAnterior(ledger, from, openingBalance);
        List<CashFlowAggregator.FlowEntry> within = ledger.stream()
                .filter(entry -> (from == null) || !entry.date.isBefore(from))
                .filter(entry -> matches(entry, type, sourceType, clientType, clientId, query))
                .map(entry -> new CashFlowAggregator.FlowEntry(entry.type, entry.date, entry.amount))
                .toList();

        CashFlowAggregator.Totals totals = CashFlowAggregator.totals(within);
        List<CashFlowSummaryResponse.PeriodRow> periods =
                CashFlowAggregator.groupByPeriod(within, granularity, saldoAnterior).stream()
                        .map(row -> new CashFlowSummaryResponse.PeriodRow(
                                row.periodStart(), row.label(), row.entradas(), row.saidas(),
                                row.resultado(), row.count(), row.saldoAcumulado()))
                        .toList();

        return new CashFlowSummaryResponse(
                from, to,
                scale(openingBalance),
                scale(saldoAnterior),
                totals.entradas(), totals.saidas(), totals.resultado(),
                totals.entradasCount(), totals.saidasCount(),
                periods);
    }

    // ---------------------------------------------------------------------
    // Lançamentos consolidados (ledger paginado)
    // ---------------------------------------------------------------------

    /**
     * Lançamentos consolidados do fluxo de caixa no período, ordenados
     * por data (mais recente primeiro) e paginados em memória. O
     * {@code balanceAfter} de cada lançamento é o saldo real do caixa
     * após o movimento — considera todos os movimentos, independentemente
     * dos filtros aplicados.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CashFlowEntryResponse> entries(LocalDate from, LocalDate to,
                                                        CashFlowEntryType type,
                                                        CashFlowDocumentSource sourceType,
                                                        CashFlowClientType clientType,
                                                        Long clientId,
                                                        String query,
                                                        int page, int size) {
        validatePeriod(from, to);
        LocalDate effectiveTo = (to != null) ? to : MAX_DATE;
        BigDecimal openingBalance = getOpeningBalance();
        List<LedgerEntry> ledger = buildLedger(effectiveTo, openingBalance);

        List<CashFlowEntryResponse> filtered = new ArrayList<>(ledger.stream()
                .filter(entry -> (from == null) || !entry.date.isBefore(from))
                .filter(entry -> matches(entry, type, sourceType, clientType, clientId, query))
                .map(this::toResponse)
                .toList());
        Collections.reverse(filtered);

        int effectiveSize = (size > 0) ? size : 20;
        int effectivePage = Math.max(page, 0);
        int totalElements = filtered.size();
        int totalPages = (totalElements == 0)
                ? 0
                : (int) Math.ceil((double) totalElements / effectiveSize);
        int fromIndex = effectivePage * effectiveSize;
        List<CashFlowEntryResponse> content = (fromIndex >= totalElements)
                ? List.of()
                : filtered.subList(fromIndex, Math.min(fromIndex + effectiveSize, totalElements));

        return new PagedResponse<>(
                content,
                effectivePage,
                effectiveSize,
                totalElements,
                totalPages,
                effectivePage == 0,
                effectivePage >= totalPages - 1);
    }

    // ---------------------------------------------------------------------
    // Typeahead de clientes (PF + PJ)
    // ---------------------------------------------------------------------

    /**
     * Typeahead de clientes (pessoas físicas e jurídicas ativas) para o
     * filtro de cliente da tela de fluxo de caixa. Delega ao serviço
     * compartilhado de busca de clientes dos módulos comerciais.
     */
    @Transactional(readOnly = true)
    public List<ClientSummaryResponse> searchClients(String query, Integer limit) {
        return clientSearchService.search(query, limit);
    }

    // ---------------------------------------------------------------------
    // Construção do ledger consolidado
    // ---------------------------------------------------------------------

    /**
     * Carrega os pagamentos efetivados até {@code effectiveTo} das duas
     * pontas financeiras, enriquece com documento/cliente/fornecedor e
     * calcula o saldo do caixa após cada movimento (em ordem ascendente).
     * Ordenação: data asc, depois ENTRADA antes de SAIDA, depois ID.
     */
    private List<LedgerEntry> buildLedger(LocalDate effectiveTo, BigDecimal openingBalance) {
        Map<Long, Receivable> receivablesById = new HashMap<>();
        for (Receivable receivable : receivableRepository.findAll()) {
            receivablesById.put(receivable.getId(), receivable);
        }
        Map<Long, Payable> payablesById = new HashMap<>();
        for (Payable payable : payableRepository.findAll()) {
            payablesById.put(payable.getId(), payable);
        }

        ClientResolver clients = new ClientResolver();
        List<LedgerEntry> ledger = new ArrayList<>();

        // Entradas: pagamentos de contas a receber.
        for (ReceivablePayment payment : receivablePaymentRepository
                .findByPaymentDateLessThanEqual(effectiveTo)) {
            Receivable parent = receivablesById.get(payment.getReceivableId());
            if (parent == null) {
                // Pagamento de conta de outra organização (ou órfão) — ignorado.
                continue;
            }
            ledger.add(entrada(payment, parent, clients));
        }

        // Saídas: pagamentos de contas a pagar.
        for (PayablePayment payment : payablePaymentRepository
                .findByPaymentDateLessThanEqual(effectiveTo)) {
            Payable parent = payablesById.get(payment.getPayableId());
            if (parent == null) {
                continue;
            }
            ledger.add(saida(payment, parent, clients));
        }

        ledger.sort(Comparator.comparing((LedgerEntry e) -> e.date)
                .thenComparingInt(e -> e.type.ordinal())
                .thenComparingLong(e -> e.id));
        computeBalances(ledger, openingBalance);
        return ledger;
    }

    /** Mapeia um pagamento de conta a receber para lançamento de entrada. */
    private LedgerEntry entrada(ReceivablePayment payment, Receivable parent, ClientResolver clients) {
        LedgerEntry entry = new LedgerEntry(
                payment.getId(), CashFlowEntryType.ENTRADA, payment.getPaymentDate(),
                payment.getAmount(), payment.getNotes());
        CashFlowDocumentSource source =
                CashFlowDocumentSource.valueOf(parent.getSourceType().name());
        entry.sourceType = source.name();
        switch (source) {
            case SALES_ORDER -> {
                entry.sourceCode = parent.getSalesOrderCode();
                entry.sourceDocId = parent.getSalesOrderId();
            }
            case TECHNICAL_PROPOSAL -> {
                entry.sourceCode = parent.getTechnicalProposalCode();
                entry.sourceDocId = parent.getTechnicalProposalId();
            }
            case CONTRACT -> {
                entry.sourceCode = parent.getContractCode();
                entry.sourceDocId = parent.getContractId();
            }
            case MANUAL, BOLETO, PURCHASE_INVOICE -> {
                // Entradas não nascem de boleto ou nota de compra.
            }
        }
        entry.documentLabel = documentLabel(source, entry.sourceCode, true);
        entry.description = parent.getDescription();
        if (parent.getCustomerId() != null) {
            entry.clientType = CashFlowClientType.CUSTOMER.name();
            entry.clientId = parent.getCustomerId();
            ClientResolved resolved = clients.customer(parent.getCustomerId());
            entry.clientName = resolved.name();
            entry.clientCode = resolved.code();
        } else if (parent.getCompanyId() != null) {
            entry.clientType = CashFlowClientType.COMPANY.name();
            entry.clientId = parent.getCompanyId();
            ClientResolved resolved = clients.company(parent.getCompanyId());
            entry.clientName = resolved.name();
            entry.clientCode = resolved.code();
        }
        return entry;
    }

    /** Mapeia um pagamento de conta a pagar para lançamento de saída. */
    private LedgerEntry saida(PayablePayment payment, Payable parent, ClientResolver clients) {
        LedgerEntry entry = new LedgerEntry(
                payment.getId(), CashFlowEntryType.SAIDA, payment.getPaymentDate(),
                payment.getAmount(), payment.getNotes());
        CashFlowDocumentSource source =
                CashFlowDocumentSource.valueOf(parent.getSourceType().name());
        entry.sourceType = source.name();
        switch (source) {
            case PURCHASE_INVOICE -> {
                entry.sourceCode = parent.getPurchaseInvoiceNumber();
                entry.sourceDocId = parent.getPurchaseInvoiceId();
            }
            case BOLETO -> entry.sourceDocId = parent.getBoletoId();
            case MANUAL, SALES_ORDER, TECHNICAL_PROPOSAL, CONTRACT -> {
                // Saídas não nascem de documentos de recebimento.
            }
        }
        entry.documentLabel = documentLabel(source, entry.sourceCode, false);
        entry.description = parent.getDescription();
        entry.counterpartyName = clients.supplier(parent.getSupplierId()).name();
        return entry;
    }

    /** Rótulo pt-BR do documento de origem (ex.: "Pedido de Venda PV-2800-2026"). */
    private static String documentLabel(CashFlowDocumentSource source, String sourceCode,
                                        boolean entrada) {
        String base = switch (source) {
            case SALES_ORDER -> "Pedido de Venda";
            case TECHNICAL_PROPOSAL -> "Proposta Técnica";
            case CONTRACT -> "Contrato";
            case MANUAL -> entrada ? "Recebimento Manual" : "Pagamento Manual";
            case BOLETO -> "Boleto";
            case PURCHASE_INVOICE -> "Nota de Compra (NF-e)";
        };
        return (sourceCode != null && !sourceCode.isBlank()) ? base + " " + sourceCode : base;
    }

    /**
     * Calcula o saldo real do caixa após cada movimento, em ordem
     * ascendente, a partir do saldo inicial.
     */
    private static void computeBalances(List<LedgerEntry> ledger, BigDecimal openingBalance) {
        BigDecimal running = (openingBalance != null) ? openingBalance : BigDecimal.ZERO;
        for (LedgerEntry entry : ledger) {
            BigDecimal amount = (entry.amount != null) ? entry.amount : BigDecimal.ZERO;
            running = (entry.type == CashFlowEntryType.ENTRADA)
                    ? running.add(amount)
                    : running.subtract(amount);
            entry.balanceAfter = scale(running);
        }
    }

    /**
     * Saldo do caixa no início do período: saldo após o último movimento
     * anterior a {@code from}, ou o saldo inicial quando não há
     * movimentos anteriores (ou o período é aberto no início).
     */
    private static BigDecimal saldoAnterior(List<LedgerEntry> ledger, LocalDate from,
                                            BigDecimal openingBalance) {
        if (from == null) {
            return (openingBalance != null) ? openingBalance : BigDecimal.ZERO;
        }
        BigDecimal priorNet = BigDecimal.ZERO;
        for (LedgerEntry entry : ledger) {
            if (!entry.date.isBefore(from)) {
                break;
            }
            BigDecimal amount = (entry.amount != null) ? entry.amount : BigDecimal.ZERO;
            priorNet = (entry.type == CashFlowEntryType.ENTRADA)
                    ? priorNet.add(amount)
                    : priorNet.subtract(amount);
        }
        return (openingBalance != null ? openingBalance : BigDecimal.ZERO).add(priorNet);
    }

    // ---------------------------------------------------------------------
    // Filtros em memória
    // ---------------------------------------------------------------------

    /**
     * Filtro de lançamentos por tipo, documento de origem, cliente e
     * busca livre. Filtros de cliente aplicam-se apenas a entradas
     * (saídas têm fornecedor como contraparte), de modo que com um
     * filtro de cliente ativo apenas entradas são exibidas.
     */
    private static boolean matches(LedgerEntry entry,
                                   CashFlowEntryType type,
                                   CashFlowDocumentSource sourceType,
                                   CashFlowClientType clientType,
                                   Long clientId,
                                   String query) {
        if (type != null && entry.type != type) {
            return false;
        }
        if (sourceType != null && !sourceType.name().equals(entry.sourceType)) {
            return false;
        }
        if (clientType != null || clientId != null) {
            if (entry.type != CashFlowEntryType.ENTRADA) {
                return false;
            }
            if (clientType != null && !clientType.name().equals(entry.clientType)) {
                return false;
            }
            if (clientId != null && !clientId.equals(entry.clientId)) {
                return false;
            }
        }
        if (query != null && !query.isBlank()) {
            String needle = query.trim().toLowerCase();
            return contains(entry.clientName, needle)
                    || contains(entry.clientCode, needle)
                    || contains(entry.sourceCode, needle)
                    || contains(entry.documentLabel, needle)
                    || contains(entry.counterpartyName, needle)
                    || contains(entry.description, needle)
                    || contains(entry.notes, needle);
        }
        return true;
    }

    private static boolean contains(String value, String needleLower) {
        return value != null && value.toLowerCase().contains(needleLower);
    }

    // ---------------------------------------------------------------------
    // Mapeamento para DTO
    // ---------------------------------------------------------------------

    private CashFlowEntryResponse toResponse(LedgerEntry entry) {
        return new CashFlowEntryResponse(
                entry.id,
                entry.type.name(),
                entry.date,
                scale(entry.amount),
                entry.sourceType,
                entry.sourceCode,
                entry.documentLabel,
                entry.sourceDocId,
                entry.clientType,
                entry.clientId,
                entry.clientName,
                entry.clientCode,
                entry.counterpartyName,
                entry.description,
                entry.notes,
                entry.balanceAfter);
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value != null) ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /** Saldo inicial corrente da organização (ZERO quando não configurado). */
    private BigDecimal getOpeningBalance() {
        Long organizationId = OrganizationContext.get();
        if (organizationId == null) {
            return BigDecimal.ZERO;
        }
        return settingsRepository.findByOrganizationId(organizationId)
                .map(CashFlowSettings::getOpeningBalance)
                .map(balance -> balance.setScale(2, RoundingMode.HALF_UP))
                .orElseGet(() -> BigDecimal.ZERO.setScale(2));
    }

    private static void validatePeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final do período.");
        }
    }

    // ---------------------------------------------------------------------
    // Tipos auxiliares internos
    // ---------------------------------------------------------------------

    /**
     * Lançamento consolidado do ledger (entrada ou saída), com os dados
     * resolvidos do documento de origem. Mutável apenas em
     * {@code balanceAfter}, calculado em ordem ascendente pelo service.
     */
    private static final class LedgerEntry {
        private final Long id;
        private final CashFlowEntryType type;
        private final LocalDate date;
        private final BigDecimal amount;
        private final String notes;
        private String sourceType;
        private String sourceCode;
        private String documentLabel;
        private Long sourceDocId;
        private String clientType;
        private Long clientId;
        private String clientName;
        private String clientCode;
        private String counterpartyName;
        private String description;
        private BigDecimal balanceAfter;

        LedgerEntry(Long id, CashFlowEntryType type, LocalDate date, BigDecimal amount,
                    String notes) {
            this.id = id;
            this.type = type;
            this.date = date;
            this.amount = amount;
            this.notes = notes;
        }
    }

    /** Dados resolvidos de cliente/fornecedor. */
    private record ClientResolved(String name, String code) {
    }

    /**
     * Resolvedor de clientes/fornecedores com cache por requisição, para
     * não repetir lookups de nomes em lançamentos do mesmo cliente.
     */
    private final class ClientResolver {
        private final Map<Long, ClientResolved> customers = new HashMap<>();
        private final Map<Long, ClientResolved> companies = new HashMap<>();
        private final Map<Long, ClientResolved> suppliers = new HashMap<>();

        ClientResolved customer(Long id) {
            return customers.computeIfAbsent(id, idValue ->
                    customerRepository.findById(idValue)
                            .map(c -> new ClientResolved(c.getName(), c.getCode()))
                            .orElse(new ClientResolved(null, null)));
        }

        ClientResolved company(Long id) {
            return companies.computeIfAbsent(id, idValue ->
                    companyRepository.findById(idValue)
                            .map(c -> new ClientResolved(
                                    (c.getTradeName() != null && !c.getTradeName().isBlank())
                                            ? c.getTradeName()
                                            : c.getLegalName(),
                                    c.getCode()))
                            .orElse(new ClientResolved(null, null)));
        }

        ClientResolved supplier(Long id) {
            return suppliers.computeIfAbsent(id, idValue ->
                    supplierRepository.findById(idValue)
                            .map(s -> new ClientResolved(
                                    (s.getTradeName() != null && !s.getTradeName().isBlank())
                                            ? s.getTradeName()
                                            : s.getLegalName(),
                                    null))
                            .orElse(new ClientResolved(null, null)));
        }
    }
}