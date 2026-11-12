package br.com.toppower.erp_toppower.contractfinances.service;

import br.com.toppower.erp_toppower.company.entity.Company;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import br.com.toppower.erp_toppower.contract.exception.ContractNotFoundException;
import br.com.toppower.erp_toppower.contract.repository.ContractRepository;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryCreateRequest;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryResponse;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryUpdateRequest;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinancesSummaryResponse;
import br.com.toppower.erp_toppower.contractfinances.entity.ContractFinanceControl;
import br.com.toppower.erp_toppower.contractfinances.entity.ContractFinanceEntry;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryStatus;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
import br.com.toppower.erp_toppower.contractfinances.exception.InvalidContractFinanceContractException;
import br.com.toppower.erp_toppower.contractfinances.repository.ContractFinanceControlRepository;
import br.com.toppower.erp_toppower.contractfinances.repository.ContractFinanceEntryRepository;
import br.com.toppower.erp_toppower.customer.entity.Customer;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários de {@link ContractFinancesService} (financeiro de
 * contratos). Usa Mockito para isolar o service dos repositories.
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>Criação do controle por contrato: retrato comercial (código,
 *       cliente PF/PJ, data, valor) e idempotência;</li>
 *   <li>Resolução do cliente PJ (nome fantasia preferido);</li>
 *   <li>Criação de lançamento: validação de contrato INATIVO, criação
 *       do controle quando faltar e persistência do lançamento;</li>
 *   <li>Edição parcial (PATCH), soft delete e reativação;</li>
 *   <li>Agregações: totais por controle e resumo consolidado por tipo;</li>
 *   <li>Contrato inexistente.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ContractFinancesServiceTest {

    @Mock
    private ContractFinanceControlRepository controlRepository;
    @Mock
    private ContractFinanceEntryRepository entryRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private ContractFinancesService service;

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private static Contract contract(Long id, Long customerId, ContractStatus status) {
        Contract c = new Contract();
        c.setId(id);
        c.setPrefix("CL");
        c.setSequence(1L);
        c.setYear(2026);
        c.setCustomerId(customerId);
        c.setStatus(status);
        c.setValidityDate(LocalDate.of(2026, 5, 1));
        c.setPrice(new BigDecimal("10000.00"));
        return c;
    }

    private static ContractFinanceControl control(Long id, Long contractId, String code) {
        ContractFinanceControl control = new ContractFinanceControl();
        control.setId(id);
        control.setContractId(contractId);
        control.setContractCode(code);
        control.setClientType("CUSTOMER");
        control.setClientName("JOÃO DA SILVA");
        control.setClientCode("CLI000001");
        control.setContractDate(LocalDate.of(2026, 5, 1));
        control.setContractValue(new BigDecimal("10000.00"));
        return control;
    }

    private static Customer customer(Long id) {
        Customer c = new Customer();
        c.setId(id);
        c.setName("JOÃO DA SILVA");
        c.setCode("CLI000001");
        return c;
    }

    // ------------------------------------------------------------------
    // Controle por contrato (hooks)
    // ------------------------------------------------------------------

    @Test
    void ensureControl_contratoNovo_criaRetratoComClientePF() {
        Contract c = contract(42L, 7L, ContractStatus.ATIVO);
        when(controlRepository.findByContractId(42L)).thenReturn(Optional.empty());
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer(7L)));
        when(controlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractFinanceControl control = service.ensureControlForContract(c).orElseThrow();

        assertEquals(42L, control.getContractId());
        assertEquals("CL-001-2026", control.getContractCode());
        assertEquals("CUSTOMER", control.getClientType());
        assertEquals("JOÃO DA SILVA", control.getClientName());
        assertEquals("CLI000001", control.getClientCode());
        assertEquals(LocalDate.of(2026, 5, 1), control.getContractDate());
        assertEquals(new BigDecimal("10000.00"), control.getContractValue());
    }

    @Test
    void ensureControl_contratoExistente_naoDuplica() {
        Contract c = contract(42L, 7L, ContractStatus.ATIVO);
        when(controlRepository.findByContractId(42L)).thenReturn(Optional.of(control(10L, 42L, "CL-001-2026")));

        ContractFinanceControl control = service.ensureControlForContract(c).orElseThrow();

        assertEquals(10L, control.getId());
        verify(controlRepository, never()).save(any());
    }

    @Test
    void ensureControl_clientePJ_prefereNomeFantasia() {
        Contract c = contract(43L, null, ContractStatus.ATIVO);
        c.setCompanyId(9L);
        Company company = new Company();
        company.setId(9L);
        company.setLegalName("TOP POWER ENGENHARIA LTDA");
        company.setTradeName("TOP POWER ENGENHARIA");
        company.setCode("EMP000009");
        when(controlRepository.findByContractId(43L)).thenReturn(Optional.empty());
        when(companyRepository.findById(9L)).thenReturn(Optional.of(company));
        when(controlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractFinanceControl control = service.ensureControlForContract(c).orElseThrow();

        assertEquals("COMPANY", control.getClientType());
        assertEquals("TOP POWER ENGENHARIA", control.getClientName());
        assertEquals("EMP000009", control.getClientCode());
        verify(customerRepository, never()).findById(any());
    }

    // ------------------------------------------------------------------
    // Lançamentos
    // ------------------------------------------------------------------

    @Test
    void createEntry_contratoInativo_lancaExcecao() {
        Contract c = contract(42L, 7L, ContractStatus.INATIVO);
        when(contractRepository.findById(42L)).thenReturn(Optional.of(c));

        assertThrows(InvalidContractFinanceContractException.class, () ->
                service.createEntry(new ContractFinanceEntryCreateRequest(
                        42L, ContractFinanceEntryType.MATERIAL, LocalDate.of(2026, 5, 10),
                        new BigDecimal("100.00"), "Cabo 50mm", null)));

        verify(entryRepository, never()).save(any());
        verify(controlRepository, never()).save(any());
    }

    @Test
    void createEntry_contratoInexistente_lancaContratoNaoEncontrado() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ContractNotFoundException.class, () ->
                service.createEntry(new ContractFinanceEntryCreateRequest(
                        99L, ContractFinanceEntryType.MATERIAL, LocalDate.of(2026, 5, 10),
                        new BigDecimal("100.00"), null, null)));
    }

    @Test
    void createEntry_comControleExistente_persisteLancamentoAtivo() {
        Contract c = contract(42L, 7L, ContractStatus.ATIVO);
        ContractFinanceControl existing = control(10L, 42L, "CL-001-2026");
        when(contractRepository.findById(42L)).thenReturn(Optional.of(c));
        when(controlRepository.findByContractId(42L)).thenReturn(Optional.of(existing));
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractFinanceEntryResponse response = service.createEntry(
                new ContractFinanceEntryCreateRequest(
                        42L, ContractFinanceEntryType.RECEBIDO, LocalDate.of(2026, 5, 10),
                        new BigDecimal("5000.00"), "Entrada inicial", null));

        ArgumentCaptor<ContractFinanceEntry> captor = ArgumentCaptor.forClass(ContractFinanceEntry.class);
        verify(entryRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getControlId());
        assertEquals(ContractFinanceEntryStatus.ATIVO, captor.getValue().getStatus());
        assertEquals(new BigDecimal("5000.00"), captor.getValue().getAmount());
        assertEquals("CL-001-2026", response.contractCode());
        assertEquals("JOÃO DA SILVA", response.clientName());
        assertEquals(42L, response.contractId());
    }

    @Test
    void createEntry_semControle_criaControleAntesDoLancamento() {
        Contract c = contract(42L, 7L, ContractStatus.ATIVO);
        when(contractRepository.findById(42L)).thenReturn(Optional.of(c));
        when(controlRepository.findByContractId(42L)).thenReturn(Optional.empty());
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer(7L)));
        when(controlRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createEntry(new ContractFinanceEntryCreateRequest(
                42L, ContractFinanceEntryType.MATERIAL, LocalDate.of(2026, 5, 10),
                new BigDecimal("250.00"), "Cabo", null));

        ArgumentCaptor<ContractFinanceControl> controlCaptor =
                ArgumentCaptor.forClass(ContractFinanceControl.class);
        verify(controlRepository).save(controlCaptor.capture());
        assertEquals(42L, controlCaptor.getValue().getContractId());
        verify(entryRepository).save(any(ContractFinanceEntry.class));
    }

    @Test
    void updateEntry_aplicaPatchParcial_semMudarStatus() {
        ContractFinanceEntry entry = new ContractFinanceEntry();
        entry.setId(5L);
        entry.setControlId(10L);
        entry.setType(ContractFinanceEntryType.MATERIAL);
        entry.setEntryDate(LocalDate.of(2026, 5, 10));
        entry.setAmount(new BigDecimal("100.00"));
        entry.setDescription("Cabo");
        entry.setStatus(ContractFinanceEntryStatus.ATIVO);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(entryRepository.save(entry)).thenReturn(entry);
        when(controlRepository.findById(10L)).thenReturn(Optional.of(control(10L, 42L, "CL-001-2026")));

        ContractFinanceEntryResponse response = service.updateEntry(5L,
                new ContractFinanceEntryUpdateRequest(null, LocalDate.of(2026, 5, 12),
                        new BigDecimal("150.50"), "Cabo 50mm"));

        assertEquals(ContractFinanceEntryType.MATERIAL, response.type());
        assertEquals(LocalDate.of(2026, 5, 12), response.entryDate());
        assertEquals(new BigDecimal("150.50"), response.amount());
        assertEquals("Cabo 50mm", response.description());
        assertEquals(ContractFinanceEntryStatus.ATIVO, entry.getStatus());
    }

    @Test
    void softDeleteEntry_marcaInativo_eActivateRestaura() {
        ContractFinanceEntry entry = new ContractFinanceEntry();
        entry.setId(5L);
        entry.setControlId(10L);
        entry.setStatus(ContractFinanceEntryStatus.ATIVO);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(entryRepository.save(entry)).thenReturn(entry);

        service.softDeleteEntry(5L);
        assertEquals(ContractFinanceEntryStatus.INATIVO, entry.getStatus());

        when(controlRepository.findById(10L)).thenReturn(Optional.of(control(10L, 42L, "CL-001-2026")));
        ContractFinanceEntryResponse response = service.activateEntry(5L);
        assertEquals(ContractFinanceEntryStatus.ATIVO, response.status());
    }

    // ------------------------------------------------------------------
    // Agregações
    // ------------------------------------------------------------------

    @Test
    void summary_somaRecebidoGastoEResultadoPorTipo() {
        when(entryRepository.aggregateTotalsByType(null, null, null, null,
                ContractFinanceEntryStatus.ATIVO)).thenReturn(List.of(
                new Object[]{ContractFinanceEntryType.RECEBIDO, new BigDecimal("500.00"), 2L},
                new Object[]{ContractFinanceEntryType.MATERIAL, new BigDecimal("200.00"), 1L}));

        ContractFinancesSummaryResponse summary = service.summary(null, null, null, null);

        assertEquals(new BigDecimal("500.00"), summary.totalRecebido());
        assertEquals(new BigDecimal("200.00"), summary.totalGasto());
        assertEquals(new BigDecimal("300.00"), summary.resultado());
        assertEquals(3L, summary.entriesCount());
        assertEquals(2, summary.porTipo().size());
        assertEquals(ContractFinanceEntryType.RECEBIDO, summary.porTipo().get(0).type());
        assertEquals(ContractFinanceEntryType.MATERIAL, summary.porTipo().get(1).type());
    }

    @Test
    void summary_semMovimentos_zero() {
        when(entryRepository.aggregateTotalsByType(null, null, null, null,
                ContractFinanceEntryStatus.ATIVO)).thenReturn(List.of());

        ContractFinancesSummaryResponse summary = service.summary(null, null, null, null);

        assertEquals(new BigDecimal("0.00"), summary.totalRecebido());
        assertEquals(new BigDecimal("0.00"), summary.totalGasto());
        assertEquals(0L, summary.entriesCount());
    }

    @Test
    void listControls_mesclaTotaisPorControle() {
        Pageable pageable = PageRequest.of(0, 20);
        ContractFinanceControl c1 = control(10L, 42L, "CL-001-2026");
        ContractFinanceControl c2 = control(11L, 43L, "CL-002-2026");
        when(controlRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(c1, c2), pageable, 2));
        when(entryRepository.aggregateByControlIds(anyCollection(),
                eq(ContractFinanceEntryStatus.ATIVO))).thenReturn(List.of(
                new Object[]{10L, ContractFinanceEntryType.RECEBIDO, new BigDecimal("500.00"), 2L},
                new Object[]{10L, ContractFinanceEntryType.MATERIAL, new BigDecimal("200.00"), 1L},
                new Object[]{11L, ContractFinanceEntryType.TERCEIROS, new BigDecimal("100.00"), 1L}));

        var result = service.listControls(null, pageable);

        assertEquals(2, result.content().size());
        var first = result.content().get(0);
        assertEquals("CL-001-2026", first.contractCode());
        assertEquals(new BigDecimal("500.00"), first.totalRecebido());
        assertEquals(new BigDecimal("200.00"), first.totalGasto());
        assertEquals(new BigDecimal("300.00"), first.resultado());
        assertEquals(3L, first.entriesCount());
        var second = result.content().get(1);
        assertEquals(new BigDecimal("0.00"), second.totalRecebido());
        assertEquals(new BigDecimal("100.00"), second.totalGasto());
        assertEquals(new BigDecimal("-100.00"), second.resultado());
        assertEquals(1L, second.entriesCount());
    }
}
