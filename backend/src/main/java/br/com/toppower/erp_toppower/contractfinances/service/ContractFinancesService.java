package br.com.toppower.erp_toppower.contractfinances.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import br.com.toppower.erp_toppower.contract.exception.ContractNotFoundException;
import br.com.toppower.erp_toppower.contract.repository.ContractRepository;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceControlResponse;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryBatchCreateRequest;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryCreateRequest;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryResponse;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryUpdateRequest;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinancesSummaryResponse;
import br.com.toppower.erp_toppower.contractfinances.entity.ContractFinanceControl;
import br.com.toppower.erp_toppower.contractfinances.entity.ContractFinanceEntry;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryStatus;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
import br.com.toppower.erp_toppower.contractfinances.exception.ContractFinanceControlNotFoundException;
import br.com.toppower.erp_toppower.contractfinances.exception.ContractFinanceEntryNotFoundException;
import br.com.toppower.erp_toppower.contractfinances.exception.InvalidContractFinanceContractException;
import br.com.toppower.erp_toppower.contractfinances.mapper.ContractFinancesMapper;
import br.com.toppower.erp_toppower.contractfinances.repository.ContractFinanceControlRepository;
import br.com.toppower.erp_toppower.contractfinances.repository.ContractFinanceEntryRepository;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Regras de negócio do financeiro de contratos: controla gastos e
 * recebimentos lançados contra contratos.
 *
 * <ul>
 *   <li><b>Controle automático por contrato</b>: {@link #createControlForContract}
 *       é chamado pelo {@code ContractService} quando um contrato é criado,
 *       registrando o retrato comercial (código, cliente, data de vigência e
 *       valor); {@link #syncControlWithContract} é chamado na atualização do
 *       contrato para manter o retrato coerente. Idempotente: um contrato tem
 *       no máximo um controle;</li>
 *   <li><b>Lançamentos</b>: gasto ou recebimento (RECEBIDO, MATERIAL,
 *       COMBUSTIVEL, REFEICAO, PEDAGIO, TERCEIROS, OUTROS, SALARIOS) com
 *       data, valor e descrição, vinculado ao controle do contrato. Apenas
 *       contratos ATIVO/CONCLUIDO aceitam lançamentos (INATIVO é
 *       bloqueado). Soft delete marca INATIVO;</li>
 *   <li><b>Totais</b>: apenas lançamentos ATIVO somam nos totais
 *       (recebido, gasto e resultado = recebido − gasto), por controle ou
 *       agregados com os filtros da lista.</li>
 * </ul>
 */
@Service
public class ContractFinancesService {

    private static final Logger log = LoggerFactory.getLogger(ContractFinancesService.class);

    private final ContractFinanceControlRepository controlRepository;
    private final ContractFinanceEntryRepository entryRepository;
    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;

    public ContractFinancesService(ContractFinanceControlRepository controlRepository,
                                   ContractFinanceEntryRepository entryRepository,
                                   ContractRepository contractRepository,
                                   CustomerRepository customerRepository,
                                   CompanyRepository companyRepository) {
        this.controlRepository = controlRepository;
        this.entryRepository = entryRepository;
        this.contractRepository = contractRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
    }

    // ---------------------------------------------------------------------
    // Controle por contrato (hook do ContractService)
    // ---------------------------------------------------------------------

    /**
     * Garante o controle do financeiro para o contrato: cria o retrato
     * comercial (código, cliente, data, valor) quando ainda não existe.
     * Chamado pelo {@code ContractService} na criação do contrato e,
     * preventivamente, na atualização e ao lançar (robustez para dados
     * criados antes do módulo). Idempotente.
     */
    @Transactional
    public Optional<ContractFinanceControl> ensureControlForContract(Contract contract) {
        if (contract == null || contract.getId() == null) {
            return Optional.empty();
        }
        Optional<ContractFinanceControl> existing = controlRepository.findByContractId(contract.getId());
        if (existing.isPresent()) {
            return existing;
        }
        ContractFinanceControl control = new ContractFinanceControl();
        control.setContractId(contract.getId());
        control.setContractCode(contract.formattedCode());
        ClientResolved client = resolveClient(contract);
        control.setClientType(client.type());
        control.setClientCode(client.code());
        control.setClientName(client.name());
        control.setContractDate(contract.getValidityDate());
        control.setContractValue(contract.getPrice());
        log.info("Criando controle financeiro para o contrato {}", contract.formattedCode());
        return Optional.of(controlRepository.save(control));
    }

    /** Alias semântico para o hook de criação de contrato. */
    @Transactional
    public Optional<ContractFinanceControl> createControlForContract(Contract contract) {
        return ensureControlForContract(contract);
    }

    /**
     * Sincroniza o controle do contrato com o retrato comercial atual
     * (data de vigência, valor e cliente — todos podem mudar no update
     * do contrato). Chamado pelo {@code ContractService.update}. Se o
     * contrato ainda não tiver controle, cria.
     */
    @Transactional
    public ContractFinanceControl syncControlWithContract(Contract contract) {
        ContractFinanceControl control = ensureControlForContract(contract)
                .orElseThrow(() -> new ContractFinanceControlNotFoundException(
                        (contract != null && contract.getId() != null) ? contract.getId() : 0L));
        control.setContractDate(contract.getValidityDate());
        control.setContractValue(contract.getPrice());
        ClientResolved client = resolveClient(contract);
        control.setClientType(client.type());
        control.setClientCode(client.code());
        control.setClientName(client.name());
        log.info("Sincronizando controle financeiro do contrato {}", contract.formattedCode());
        return controlRepository.save(control);
    }

    // ---------------------------------------------------------------------
    // Controles — listagem e detalhe
    // ---------------------------------------------------------------------

    /**
     * Lista paginada dos controles de contrato, com busca opcional por
     * código do contrato ou nome do cliente, já com os totais agregados
     * dos lançamentos ATIVO (recebido, gasto, resultado e contagem).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ContractFinanceControlResponse> listControls(String query, Pageable pageable) {
        String trimmed = (query != null && !query.isBlank()) ? query.trim() : null;
        Page<ContractFinanceControl> page = (trimmed == null)
                ? controlRepository.findAll(pageable)
                : controlRepository.searchControls(trimmed, pageable);
        Map<Long, Totals> totalsById = totalsForControls(page.getContent());
        Page<ContractFinanceControlResponse> mapped = page.map(control -> {
            Totals t = totalsById.getOrDefault(control.getId(), new Totals());
            return ContractFinancesMapper.toResponse(control, t.recebido(), t.gasto(), t.count());
        });
        return PagedResponse.from(mapped);
    }

    /** Detalhe do controle com os totais agregados dos lançamentos ATIVO. */
    @Transactional(readOnly = true)
    public ContractFinanceControlResponse getControl(Long id) {
        ContractFinanceControl control = controlRepository.findById(id)
                .orElseThrow(() -> new ContractFinanceControlNotFoundException(id));
        Totals totals = totalsForControls(List.of(control))
                .getOrDefault(control.getId(), new Totals());
        return ContractFinancesMapper.toResponse(control, totals.recebido(), totals.gasto(), totals.count());
    }

    // ---------------------------------------------------------------------
    // Lançamentos
    // ---------------------------------------------------------------------

    /**
     * Lança um gasto ou recebimento no financeiro de um contrato. Resolve
     * (ou cria, se faltar) o controle, valida o status do contrato
     * (INATIVO é bloqueado) e persiste o lançamento com status ATIVO
     * (a menos que o request informe outro).
     */
    @Transactional
    public ContractFinanceEntryResponse createEntry(ContractFinanceEntryCreateRequest request) {
        ContractFinanceControl control = resolveControlForActiveContract(request.contractId());

        ContractFinanceEntry entry = ContractFinancesMapper.toEntryEntity(request, control.getId());
        ContractFinanceEntry saved = entryRepository.save(entry);
        log.info("Lançamento {} ({}) de {} no contrato {}", saved.getId(), saved.getType(),
                saved.getAmount(), control.getContractCode());
        return ContractFinancesMapper.toResponse(saved, control);
    }

    /**
     * Lança vários gastos/recebimentos de um contrato de uma vez:
     * contrato e data compartilhados, um item por natureza preenchida.
     * A transação cobre o lote — se qualquer item for inválido, nada é
     * persistido (all-or-nothing).
     */
    @Transactional
    public List<ContractFinanceEntryResponse> createEntriesBatch(
            ContractFinanceEntryBatchCreateRequest request) {
        ContractFinanceControl control = resolveControlForActiveContract(request.contractId());
        List<ContractFinanceEntry> entries =
                request.items().stream()
                        .map(item -> ContractFinancesMapper.toEntryEntity(
                                item.type(), request.entryDate(), item.amount(),
                                item.description(), null, control.getId()))
                        .toList();
        List<ContractFinanceEntry> saved = entryRepository.saveAll(entries);
        log.info("Lote com {} lançamento(s) no contrato {}", saved.size(), control.getContractCode());
        return saved.stream()
                .map(entry -> ContractFinancesMapper.toResponse(entry, control))
                .toList();
    }

    /**
     * Localiza (ou cria, se faltar) o controle do contrato e valida que
     * o contrato existe e não está INATIVO — regra compartilhada pelas
     * criações individual e em lote.
     */
    private ContractFinanceControl resolveControlForActiveContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
        if (contract.getStatus() == ContractStatus.INATIVO) {
            throw InvalidContractFinanceContractException.contractInactive(contract.getId());
        }
        return ensureControlForContract(contract)
                .orElseThrow(() -> new ContractFinanceControlNotFoundException(contract.getId()));
    }

    /** Detalhe do lançamento, com os dados do controle resolvidos. */
    @Transactional(readOnly = true)
    public ContractFinanceEntryResponse getEntry(Long id) {
        ContractFinanceEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ContractFinanceEntryNotFoundException(id));
        ContractFinanceControl control = controlRepository.findById(entry.getControlId())
                .orElse(null);
        return ContractFinancesMapper.toResponse(entry, control);
    }

    /**
     * Lista paginada de lançamentos com filtros combináveis (todos
     * opcionais), ordenada pela data mais recente. Os dados do controle
     * (código do contrato, cliente) são resolvidos em lote por página.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ContractFinanceEntryResponse> listEntries(Long controlId,
                                                                   ContractFinanceEntryType type,
                                                                   ContractFinanceEntryStatus status,
                                                                   LocalDate from, LocalDate to,
                                                                   Pageable pageable) {
        Page<ContractFinanceEntry> page = entryRepository.listEntries(
                controlId, type, status, from, to, pageable);
        Map<Long, ContractFinanceControl> controlsById = controlsFor(page.getContent());
        Page<ContractFinanceEntryResponse> mapped = page.map(entry ->
                ContractFinancesMapper.toResponse(entry, controlsById.get(entry.getControlId())));
        return PagedResponse.from(mapped);
    }

    /**
     * Edita parcialmente um lançamento (tipo, data, valor e descrição).
     * Não altera o vínculo com o contrato nem o status.
     */
    @Transactional
    public ContractFinanceEntryResponse updateEntry(Long id, ContractFinanceEntryUpdateRequest request) {
        ContractFinanceEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ContractFinanceEntryNotFoundException(id));
        ContractFinancesMapper.applyEntryUpdate(entry, request);
        ContractFinanceEntry saved = entryRepository.save(entry);
        ContractFinanceControl control = controlRepository.findById(entry.getControlId())
                .orElse(null);
        return ContractFinancesMapper.toResponse(saved, control);
    }

    /** Soft delete: marca o lançamento como INATIVO (para de somar nos totais). */
    @Transactional
    public void softDeleteEntry(Long id) {
        ContractFinanceEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ContractFinanceEntryNotFoundException(id));
        entry.setStatus(ContractFinanceEntryStatus.INATIVO);
        entryRepository.save(entry);
    }

    /** Reativa um lançamento inativado (volta a somar nos totais). */
    @Transactional
    public ContractFinanceEntryResponse activateEntry(Long id) {
        ContractFinanceEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ContractFinanceEntryNotFoundException(id));
        entry.setStatus(ContractFinanceEntryStatus.ATIVO);
        ContractFinanceEntry saved = entryRepository.save(entry);
        ContractFinanceControl control = controlRepository.findById(entry.getControlId())
                .orElse(null);
        return ContractFinancesMapper.toResponse(saved, control);
    }

    // ---------------------------------------------------------------------
    // Resumo consolidado
    // ---------------------------------------------------------------------

    /**
     * Resumo consolidado (por filtros) dos lançamentos ATIVO: total
     * recebido, total gasto, resultado e quebra por natureza.
     */
    @Transactional(readOnly = true)
    public ContractFinancesSummaryResponse summary(Long controlId,
                                                   ContractFinanceEntryType type,
                                                   LocalDate from, LocalDate to) {
        List<Object[]> rows = entryRepository.aggregateTotalsByType(
                controlId, type, from, to, ContractFinanceEntryStatus.ATIVO);

        Map<ContractFinanceEntryType, ContractFinancesSummaryResponse.TypeTotal> byType = new HashMap<>();
        for (Object[] row : rows) {
            ContractFinanceEntryType rowType = (ContractFinanceEntryType) row[0];
            BigDecimal sum = scaleZero((BigDecimal) row[1]);
            long count = (row[2] instanceof Number n) ? n.longValue() : 0L;
            byType.put(rowType, new ContractFinancesSummaryResponse.TypeTotal(
                    rowType, sum, count));
        }

        // Quebra por tipo na ordem do enum (estável na UI); apenas tipos presentes.
        List<ContractFinancesSummaryResponse.TypeTotal> porTipo =
                java.util.Arrays.stream(ContractFinanceEntryType.values())
                        .filter(byType::containsKey)
                        .map(byType::get)
                        .toList();

        BigDecimal recebido = byType.getOrDefault(ContractFinanceEntryType.RECEBIDO,
                new ContractFinancesSummaryResponse.TypeTotal(ContractFinanceEntryType.RECEBIDO, BigDecimal.ZERO, 0L))
                .total();
        BigDecimal gasto = BigDecimal.ZERO;
        long count = 0L;
        for (ContractFinancesSummaryResponse.TypeTotal t : porTipo) {
            count += t.count();
            if (t.type() != ContractFinanceEntryType.RECEBIDO) {
                gasto = gasto.add(t.total());
            }
        }
        BigDecimal resultado = recebido.subtract(gasto).setScale(2, RoundingMode.HALF_UP);
        return new ContractFinancesSummaryResponse(
                scaleZero(recebido), scaleZero(gasto), resultado, count, porTipo);
    }

    // ---------------------------------------------------------------------
    // Helpers — cliente e totais
    // ---------------------------------------------------------------------

    /** Resolução do cliente do contrato em nome/código (padrão do módulo contract). */
    private record ClientResolved(String type, String code, String name) {
        static final ClientResolved EMPTY = new ClientResolved(null, null, null);
    }

    private ClientResolved resolveClient(Contract contract) {
        if (contract.getCustomerId() != null) {
            return customerRepository.findById(contract.getCustomerId())
                    .map(c -> new ClientResolved("CUSTOMER", c.getCode(), c.getName()))
                    .orElse(ClientResolved.EMPTY);
        }
        if (contract.getCompanyId() != null) {
            return companyRepository.findById(contract.getCompanyId())
                    .map(c -> new ClientResolved("COMPANY", c.getCode(),
                            (c.getTradeName() != null && !c.getTradeName().isBlank())
                                    ? c.getTradeName() : c.getLegalName()))
                    .orElse(ClientResolved.EMPTY);
        }
        return ClientResolved.EMPTY;
    }

    /** Totais (recebido/gasto/contagem) por controle, dos lançamentos ATIVO. */
    private Map<Long, Totals> totalsForControls(List<ContractFinanceControl> controls) {
        Map<Long, Totals> totalsById = new HashMap<>();
        if (controls.isEmpty()) {
            return totalsById;
        }
        List<Object[]> rows = entryRepository.aggregateByControlIds(
                controls.stream().map(ContractFinanceControl::getId).toList(),
                ContractFinanceEntryStatus.ATIVO);
        for (Object[] row : rows) {
            Long controlId = (Long) row[0];
            ContractFinanceEntryType type = (ContractFinanceEntryType) row[1];
            BigDecimal sum = scaleZero((BigDecimal) row[2]);
            long count = (row[3] instanceof Number n) ? n.longValue() : 0L;
            Totals totals = totalsById.computeIfAbsent(controlId, k -> new Totals());
            if (type == ContractFinanceEntryType.RECEBIDO) {
                totals.recebido = sum;
            } else {
                totals.gasto = totals.gasto.add(sum);
            }
            totals.count += count;
        }
        return totalsById;
    }

    /** Dados dos controles de uma página de lançamentos (resolvidos em lote). */
    private Map<Long, ContractFinanceControl> controlsFor(List<ContractFinanceEntry> entries) {
        Map<Long, ContractFinanceControl> byId = new HashMap<>();
        List<Long> controlIds = entries.stream().map(ContractFinanceEntry::getControlId).distinct().toList();
        if (controlIds.isEmpty()) {
            return byId;
        }
        for (ContractFinanceControl control : controlRepository.findAllById(controlIds)) {
            byId.put(control.getId(), control);
        }
        return byId;
    }

    /** Totais de um controle: recebido, gasto e quantidade de lançamentos ATIVO. */
    private static final class Totals {
        private BigDecimal recebido = BigDecimal.ZERO;
        private BigDecimal gasto = BigDecimal.ZERO;
        private long count = 0L;

        BigDecimal recebido() {
            return scaleZero(recebido);
        }

        BigDecimal gasto() {
            return scaleZero(gasto);
        }

        long count() {
            return count;
        }
    }

    private static BigDecimal scaleZero(BigDecimal value) {
        return ((value != null) ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
