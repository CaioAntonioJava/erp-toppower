package br.com.toppower.erp_toppower.cashflow.service;

import br.com.toppower.erp_toppower.cashflow.dto.CashFlowEntryResponse;
import br.com.toppower.erp_toppower.cashflow.dto.CashFlowSummaryResponse;
import br.com.toppower.erp_toppower.cashflow.entity.CashFlowSettings;
import br.com.toppower.erp_toppower.cashflow.enums.CashFlowClientType;
import br.com.toppower.erp_toppower.cashflow.enums.CashFlowEntryType;
import br.com.toppower.erp_toppower.cashflow.repository.CashFlowSettingsRepository;
import br.com.toppower.erp_toppower.cashflow.service.CashFlowAggregator.Granularity;
import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.customer.entity.Customer;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.repository.PayablePaymentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayableRepository;
import br.com.toppower.erp_toppower.receivable.entity.Receivable;
import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.repository.ReceivablePaymentRepository;
import br.com.toppower.erp_toppower.receivable.repository.ReceivableRepository;
import br.com.toppower.erp_toppower.sales.quotation.dto.ClientSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse;
import br.com.toppower.erp_toppower.sales.quotation.service.ClientSearchService;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testes unitários de {@link CashFlowService} (consolidação do fluxo de
 * caixa). Usa Mockito para isolar o service dos repositories, cobrindo a
 * lógica que fica fora de {@link CashFlowAggregator}:
 *
 * <ul>
 *   <li>Construção do ledger: mapeamento de pagamentos de contas a
 *       receber (entradas por SALES_ORDER) e a pagar (saídas por BOLETO
 *       e PURCHASE_INVOICE), com rótulo do documento e cliente/fornecedor
 *       resolvidos;</li>
 *   <li>Saldo do caixa após cada movimento (considera todos os
 *       movimentos, mesmo com filtros ativos);</li>
 *   <li>Resumo: saldo anterior, totais por tipo e série por período;</li>
 *   <li>Filtros: tipo, cliente (exclui saídas) e busca livre;</li>
 *   <li>Paginação e ordenação pela data mais recente;</li>
 *   <li>Validação de período.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CashFlowServiceTest {

    @Mock
    private CashFlowSettingsRepository settingsRepository;
    @Mock
    private ReceivableRepository receivableRepository;
    @Mock
    private ReceivablePaymentRepository receivablePaymentRepository;
    @Mock
    private PayableRepository payableRepository;
    @Mock
    private PayablePaymentRepository payablePaymentRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ClientSearchService clientSearchService;

    @InjectMocks
    private CashFlowService service;

    @BeforeEach
    void setUpOrganization() {
        OrganizationContext.set(1L);
    }

    @AfterEach
    void clearOrganization() {
        OrganizationContext.clear();
    }

    // ------------------------------------------------------------------
    // Fixtures — cenário: saldo inicial 1000, entrada de 500 (pedido de
    // venda em 10/05) e duas saídas (NF-e 200 em 04/05, boleto 300 em
    // 15/05).
    // ------------------------------------------------------------------

    private void stubScenario() {
        when(settingsRepository.findByOrganizationId(1L))
                .thenReturn(Optional.of(settingsComSaldo("1000.00")));
        when(receivableRepository.findAll()).thenReturn(List.of(receivableVenda()));
        when(payableRepository.findAll()).thenReturn(List.of(payableBoleto(), payableNfe()));
        when(receivablePaymentRepository.findByPaymentDateLessThanEqual(any()))
                .thenReturn(List.of(entradaVenda()));
        when(payablePaymentRepository.findByPaymentDateLessThanEqual(any()))
                .thenReturn(List.of(saidaNfe(), saidaBoleto()));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customerJoao()));
        when(supplierRepository.findById(2L)).thenReturn(Optional.of(supplierFornecedor()));
    }

    private static CashFlowSettings settingsComSaldo(String openingBalance) {
        CashFlowSettings s = new CashFlowSettings();
        s.setOpeningBalance(new BigDecimal(openingBalance));
        return s;
    }

    private static Receivable receivableVenda() {
        Receivable r = new Receivable();
        r.setId(100L);
        r.setDescription("Pedido de Venda PV-0001-2026");
        r.setSourceType(ReceivableSource.SALES_ORDER);
        r.setSalesOrderId(11L);
        r.setSalesOrderCode("PV-0001-2026");
        r.setCustomerId(1L);
        return r;
    }

    private static Payable payableBoleto() {
        Payable p = new Payable();
        p.setId(200L);
        p.setDescription("Boleto fornecedor");
        p.setSourceType(PayableSource.BOLETO);
        p.setBoletoId(30L);
        p.setSupplierId(2L);
        return p;
    }

    private static Payable payableNfe() {
        Payable p = new Payable();
        p.setId(201L);
        p.setDescription("Nota de compra 12345");
        p.setSourceType(PayableSource.PURCHASE_INVOICE);
        p.setPurchaseInvoiceNumber("12345");
        p.setSupplierId(2L);
        return p;
    }

    private static ReceivablePayment entradaVenda() {
        ReceivablePayment pay = new ReceivablePayment();
        pay.setId(950L);
        pay.setReceivableId(100L);
        pay.setAmount(new BigDecimal("500.00"));
        pay.setPaymentDate(LocalDate.of(2026, 5, 10));
        pay.setNotes("PIX recebido");
        return pay;
    }

    private static PayablePayment saidaBoleto() {
        PayablePayment pay = new PayablePayment();
        pay.setId(960L);
        pay.setPayableId(200L);
        pay.setAmount(new BigDecimal("300.00"));
        pay.setPaymentDate(LocalDate.of(2026, 5, 15));
        pay.setNotes("Débito automático");
        return pay;
    }

    private static PayablePayment saidaNfe() {
        PayablePayment pay = new PayablePayment();
        pay.setId(961L);
        pay.setPayableId(201L);
        pay.setAmount(new BigDecimal("200.00"));
        pay.setPaymentDate(LocalDate.of(2026, 5, 4));
        return pay;
    }

    private static Customer customerJoao() {
        Customer c = new Customer();
        c.setName("JOÃO DA SILVA");
        c.setCode("CLI000001");
        return c;
    }

    private static Supplier supplierFornecedor() {
        Supplier s = new Supplier();
        s.setLegalName("FORNECEDOR A LTDA");
        s.setTradeName("FORNECEDOR A");
        return s;
    }

    // ------------------------------------------------------------------
    // Resumo
    // ------------------------------------------------------------------

    @Test
    void summary_totaisSerieESaldoAnterior() {
        stubScenario();

        CashFlowSummaryResponse summary = service.summary(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                Granularity.MONTH, null, null, null, null, null);

        assertEquals(new BigDecimal("1000.00"), summary.openingBalance());
        // Sem movimentos anteriores a 01/05 → saldo anterior = inicial.
        assertEquals(new BigDecimal("1000.00"), summary.saldoAnterior());
        assertEquals(new BigDecimal("500.00"), summary.totalEntradas());
        assertEquals(new BigDecimal("500.00"), summary.totalSaidas());
        assertEquals(new BigDecimal("0.00"), summary.resultado());
        assertEquals(1L, summary.entradasCount());
        assertEquals(2L, summary.saidasCount());

        assertEquals(1, summary.periods().size());
        var row = summary.periods().get(0);
        assertEquals("05/2026", row.label());
        assertEquals(new BigDecimal("500.00"), row.entradas());
        assertEquals(new BigDecimal("500.00"), row.saidas());
        assertEquals(3L, row.count());
        // Saldo após o último movimento: 1000 + 500 − 200 − 300.
        assertEquals(new BigDecimal("1000.00"), row.saldoAcumulado());
    }

    @Test
    void summary_periodoInvalido_lancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> service.summary(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 1),
                Granularity.MONTH, null, null, null, null, null));
    }

    // ------------------------------------------------------------------
    // Lançamentos
    // ------------------------------------------------------------------

    @Test
    void entries_ordenadoPorDataDesc_comSaldoAposCadaMovimento() {
        stubScenario();

        PagedResponse<CashFlowEntryResponse> page =
                service.entries(null, null, null, null, null, null, null, 0, 20);

        assertEquals(3L, page.totalElements());
        // Mais recente primeiro: boleto (15/05) → venda (10/05) → NF-e (04/05).
        assertEquals(960L, page.content().get(0).id());
        assertEquals(950L, page.content().get(1).id());
        assertEquals(961L, page.content().get(2).id());
        // Saldo após cada movimento: 1000 − 200 = 800; +500 = 1300; −300 = 1000.
        assertEquals(new BigDecimal("1000.00"), page.content().get(0).balanceAfter());
        assertEquals(new BigDecimal("1300.00"), page.content().get(1).balanceAfter());
        assertEquals(new BigDecimal("800.00"), page.content().get(2).balanceAfter());
        // Rótulos de documento e cliente/fornecedor resolvidos.
        assertEquals("Boleto", page.content().get(0).documentLabel());
        assertEquals("FORNECEDOR A", page.content().get(0).counterpartyName());
        assertEquals("Pedido de Venda PV-0001-2026", page.content().get(1).documentLabel());
        assertEquals("JOÃO DA SILVA", page.content().get(1).clientName());
        assertEquals("Nota de Compra (NF-e) 12345", page.content().get(2).documentLabel());
    }

    @Test
    void entries_saldoAposIndependenteDoFiltroDeTipo() {
        stubScenario();

        PagedResponse<CashFlowEntryResponse> page = service.entries(
                null, null, CashFlowEntryType.SAIDA, null, null, null, null, 0, 20);

        assertEquals(2L, page.totalElements());
        // O saldo do lançamento de 15/05 considera a entrada de 10/05
        // (posição real do caixa), mesmo com o filtro de tipo ativo.
        assertEquals(new BigDecimal("1000.00"), page.content().get(0).balanceAfter());
    }

    @Test
    void entries_filtroCliente_aplicaApenasEmEntradas() {
        stubScenario();

        PagedResponse<CashFlowEntryResponse> page = service.entries(
                null, null, null, null, CashFlowClientType.CUSTOMER, 1L, null, 0, 20);

        assertEquals(1L, page.totalElements());
        CashFlowEntryResponse entrada = page.content().get(0);
        assertEquals(CashFlowEntryType.ENTRADA.name(), entrada.entryType());
        assertEquals("JOÃO DA SILVA", entrada.clientName());
        assertEquals("CLI000001", entrada.clientCode());
    }

    @Test
    void entries_buscaLivre_encontraClienteEDocumento() {
        stubScenario();

        PagedResponse<CashFlowEntryResponse> porCliente = service.entries(
                null, null, null, null, null, null, "JOÃO", 0, 20);
        assertEquals(1L, porCliente.totalElements());
        assertEquals(950L, porCliente.content().get(0).id());

        PagedResponse<CashFlowEntryResponse> porDocumento = service.entries(
                null, null, null, null, null, null, "boleto", 0, 20);
        assertEquals(1L, porDocumento.totalElements());
        assertEquals(960L, porDocumento.content().get(0).id());

        PagedResponse<CashFlowEntryResponse> semCorrespondencia = service.entries(
                null, null, null, null, null, null, "inexistente", 0, 20);
        assertEquals(0L, semCorrespondencia.totalElements());
    }

    @Test
    void entries_paginacaoRespeitaTamanhoEOrdem() {
        stubScenario();

        PagedResponse<CashFlowEntryResponse> primeira = service.entries(
                null, null, null, null, null, null, null, 0, 2);

        assertEquals(3L, primeira.totalElements());
        assertEquals(2, primeira.totalPages());
        assertTrue(primeira.first());
        assertFalse(primeira.last());
        assertEquals(2, primeira.content().size());
        assertEquals(960L, primeira.content().get(0).id());
        assertEquals(950L, primeira.content().get(1).id());

        PagedResponse<CashFlowEntryResponse> segunda = service.entries(
                null, null, null, null, null, null, null, 1, 2);
        assertEquals(1, segunda.content().size());
        assertEquals(961L, segunda.content().get(0).id());
    }

    // ------------------------------------------------------------------
    // Typeahead de clientes
    // ------------------------------------------------------------------

    @Test
    void searchClients_delegaAoServicoCompartilhado() {
        List<ClientSummaryResponse> expected = List.of(new ClientSummaryResponse(
                QuotationResponse.ClientType.CUSTOMER, 1L, "CLI000001", "JOÃO DA SILVA", null));
        when(clientSearchService.search("jo", 10)).thenReturn(expected);

        List<ClientSummaryResponse> result = service.searchClients("jo", 10);

        assertEquals(expected, result);
    }
}
