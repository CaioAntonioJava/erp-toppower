package br.com.toppower.erp_toppower.boleto.service;

import br.com.toppower.erp_toppower.boleto.dto.BoletoCreateRequest;
import br.com.toppower.erp_toppower.boleto.dto.BoletoResponse;
import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.boleto.exception.InvalidInstallmentPlanException;
import br.com.toppower.erp_toppower.boleto.repository.BoletoRepository;
import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.repository.PayablePaymentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayableRepository;
import br.com.toppower.erp_toppower.payable.service.PayablePaymentAttachmentService;
import br.com.toppower.erp_toppower.payable.service.PayableService;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
import br.com.toppower.erp_toppower.supplier.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de {@link BoletoService} focados na lógica de
 * parcelamento ({@code createInstallments}). Usa Mockito para isolar
 * o service de seus colaboradores (repositories, payableService, etc.).
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>Divisão do valor total com residual na última parcela</li>
 *   <li>Cálculo correto dos vencimentos (data base + prazo em dias)</li>
 *   <li>Rejeição quando a quantidade de prazos != installmentsCount</li>
 *   <li>installmentPlanId é o mesmo para todas as parcelas e não é null</li>
 *   <li>Boleto avulso (installmentsCount = 1) não define installmentPlanId</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BoletoServiceTest {

    @Mock
    private BoletoRepository boletoRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private PayableService payableService;
    @Mock
    private SupplierService supplierService;
    @Mock
    private PayableRepository payableRepository;
    @Mock
    private PayablePaymentRepository payablePaymentRepository;
    @Mock
    private PayablePaymentAttachmentService payablePaymentAttachmentService;

    @InjectMocks
    private BoletoService boletoService;

    /**
     * Constrói um request de parcelamento com 3 parcelas e prazos 30/60/90.
     */
    private BoletoCreateRequest parcelamentoRequest(BigDecimal total, LocalDate baseDate) {
        return new BoletoCreateRequest(
                "CT-001-2026",        // contractWorkNumber
                "JOAO DA SILVA",      // responsibleName
                total,                // value
                baseDate,             // dueDate
                null,                 // status (default ATIVO via @PrePersist)
                null,                 // supplierId (usa genérico)
                "NF-00123",           // invoiceNumber
                LocalDate.of(2026, 7, 20), // invoiceDate
                null,                 // installmentNumber (ignorado no parcelamento)
                3,                    // installmentsCount
                "30/60/90");          // installmentTerms
    }

    /**
     * Configura o mock do supplierService para retornar um fornecedor
     * genérico com ID 99.
     */
    private void mockGenericSupplier() {
        Supplier generic = new Supplier();
        generic.setId(99L);
        when(supplierService.findOrCreateGeneric()).thenReturn(generic);
    }

    /**
     * Configura o mock do boletoRepository.save para retornar o próprio
     * boleto (simula o comportamento do JPA) e atribuir um ID sequencial.
     */
    private void mockSaveWithIds() {
        long[] counter = {1L};
        when(boletoRepository.save(any(Boleto.class))).thenAnswer(invocation -> {
            Boleto b = invocation.getArgument(0);
            if (b.getId() == null) {
                b.setId(counter[0]++);
            }
            return b;
        });
    }

    @Test
    void createInstallments_divideValorComResidualNaUltimaParcela() {
        // 100.00 / 3 = 33.33, 33.33, 33.34 (última absorve o residual)
        BoletoCreateRequest request = parcelamentoRequest(
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 15));

        mockGenericSupplier();
        mockSaveWithIds();
        when(payableService.generateFromBoleto(any(Boleto.class)))
                .thenReturn(Optional.empty());
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        List<BoletoResponse> result = boletoService.create(request);

        assertEquals(3, result.size());
        // Verifica os valores de cada parcela.
        assertEquals(new BigDecimal("33.33"), result.get(0).value());
        assertEquals(new BigDecimal("33.33"), result.get(1).value());
        assertEquals(new BigDecimal("33.34"), result.get(2).value());
    }

    @Test
    void createInstallments_calculaVencimentosCorretamente() {
        // Base 2026-08-15 + 30/60/90 dias
        BoletoCreateRequest request = parcelamentoRequest(
                new BigDecimal("300.00"), LocalDate.of(2026, 8, 15));

        mockGenericSupplier();
        mockSaveWithIds();
        when(payableService.generateFromBoleto(any(Boleto.class)))
                .thenReturn(Optional.empty());
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        List<BoletoResponse> result = boletoService.create(request);

        assertEquals(LocalDate.of(2026, 9, 14), result.get(0).dueDate());
        assertEquals(LocalDate.of(2026, 10, 14), result.get(1).dueDate());
        assertEquals(LocalDate.of(2026, 11, 13), result.get(2).dueDate());
    }

    @Test
    void createInstallments_installmentPlanIdIgualParaTodasAsParcelas() {
        BoletoCreateRequest request = parcelamentoRequest(
                new BigDecimal("300.00"), LocalDate.of(2026, 8, 15));

        mockGenericSupplier();
        mockSaveWithIds();
        when(payableService.generateFromBoleto(any(Boleto.class)))
                .thenReturn(Optional.empty());
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        List<BoletoResponse> result = boletoService.create(request);

        // Todas as parcelas devem ter o mesmo installmentPlanId não nulo.
        String planId = result.get(0).installmentPlanId();
        assertNotNull(planId);
        assertEquals(planId, result.get(1).installmentPlanId());
        assertEquals(planId, result.get(2).installmentPlanId());
    }

    @Test
    void createInstallments_installmentNumberSequencial() {
        BoletoCreateRequest request = parcelamentoRequest(
                new BigDecimal("300.00"), LocalDate.of(2026, 8, 15));

        mockGenericSupplier();
        mockSaveWithIds();
        when(payableService.generateFromBoleto(any(Boleto.class)))
                .thenReturn(Optional.empty());
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        List<BoletoResponse> result = boletoService.create(request);

        assertEquals(1, result.get(0).installmentNumber());
        assertEquals(2, result.get(1).installmentNumber());
        assertEquals(3, result.get(2).installmentNumber());
    }

    @Test
    void createInstallments_rejeitaQuandoTermosDiferemDeInstallmentsCount() {
        // 3 parcelas mas apenas 2 prazos ("30/60")
        BoletoCreateRequest request = new BoletoCreateRequest(
                "CT-001", "RESP", new BigDecimal("300.00"),
                LocalDate.of(2026, 8, 15), null, null,
                null, null, null,
                3, "30/60");

        assertThrows(InvalidInstallmentPlanException.class,
                () -> boletoService.create(request));
    }

    @Test
    void createInstallments_rejeitaQuandoTermosVazio() {
        BoletoCreateRequest request = new BoletoCreateRequest(
                "CT-001", "RESP", new BigDecimal("300.00"),
                LocalDate.of(2026, 8, 15), null, null,
                null, null, null,
                3, "");

        assertThrows(InvalidInstallmentPlanException.class,
                () -> boletoService.create(request));
    }

    @Test
    void createSingle_naoDefineInstallmentPlanId() {
        // Boleto avulso (installmentsCount = 1) não deve ter installmentPlanId.
        BoletoCreateRequest request = new BoletoCreateRequest(
                "CT-001", "RESP", new BigDecimal("150.00"),
                LocalDate.of(2026, 8, 15), null, null,
                null, null, null,
                1, null);

        mockGenericSupplier();
        mockSaveWithIds();
        when(payableService.generateFromBoleto(any(Boleto.class)))
                .thenReturn(Optional.empty());
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        List<BoletoResponse> result = boletoService.create(request);

        assertEquals(1, result.size());
        assertNull(result.get(0).installmentPlanId());
    }

    @Test
    void createInstallments_copiaCamposIdentificacaoParaTodasAsParcelas() {
        BoletoCreateRequest request = parcelamentoRequest(
                new BigDecimal("300.00"), LocalDate.of(2026, 8, 15));

        mockGenericSupplier();
        mockSaveWithIds();
        when(payableService.generateFromBoleto(any(Boleto.class)))
                .thenReturn(Optional.empty());
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        ArgumentCaptor<Boleto> captor = ArgumentCaptor.forClass(Boleto.class);
        boletoService.create(request);
        verify(boletoRepository, times(3)).save(captor.capture());

        List<Boleto> saved = captor.getAllValues();
        for (Boleto b : saved) {
            assertEquals("CT-001-2026", b.getContractWorkNumber());
            assertEquals("JOAO DA SILVA", b.getResponsibleName());
            assertEquals("NF-00123", b.getInvoiceNumber());
            assertEquals(LocalDate.of(2026, 7, 20), b.getInvoiceDate());
            assertEquals(99L, b.getSupplierId());
        }
    }

    @Test
    void generatePayableFromBoleto_rejeitaQuandoJaExisteContaAtiva() {
        Boleto boleto = new Boleto();
        boleto.setId(1L);
        boleto.setSupplierId(10L);

        when(boletoRepository.findById(1L)).thenReturn(Optional.of(boleto));
        Payable existing = new Payable();
        existing.setId(50L);
        existing.setBoletoId(1L);
        when(payableRepository.findActiveByBoletoId(1L))
                .thenReturn(Optional.of(existing));

        // Deve lançar PayableBusinessException (boletoAlreadyLinked → 409)
        var ex = assertThrows(
                br.com.toppower.erp_toppower.payable.exception.PayableBusinessException.class,
                () -> boletoService.generatePayableFromBoleto(1L));
        assertTrue(ex.getMessage().contains("já possui uma conta a pagar ativa"));
    }

    @Test
    void generatePayableFromBoleto_rejeitaQuandoBoletoSemSupplier() {
        Boleto boleto = new Boleto();
        boleto.setId(1L);
        boleto.setSupplierId(null);

        when(boletoRepository.findById(1L)).thenReturn(Optional.of(boleto));

        var ex = assertThrows(
                br.com.toppower.erp_toppower.payable.exception.PayableBusinessException.class,
                () -> boletoService.generatePayableFromBoleto(1L));
        assertTrue(ex.getMessage().contains("não possui fornecedor vinculado"));
    }
}