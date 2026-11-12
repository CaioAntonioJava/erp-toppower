package br.com.toppower.erp_toppower.payable.service;

import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.boleto.repository.BoletoRepository;
import br.com.toppower.erp_toppower.payable.dto.PayablePaymentRequest;
import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.entity.PayableInstallment;
import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import br.com.toppower.erp_toppower.payable.repository.PayableInstallmentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayablePaymentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayableRepository;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários de {@link PayableService} focados na sincronização do
 * flag pago do boleto vinculado com o estado da conta a pagar
 * ({@code syncBoletoState}). Usa Mockito para isolar o service de seus
 * colaboradores (repositories, attachment service).
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>Estorno (removePayment) em conta originada de boleto → boleto
 *       desmarcado (paid=false, paymentDate=null);</li>
 *   <li>Estorno em conta manual → boleto não é tocado;</li>
 *   <li>Registro de pagamento que quita a conta de boleto → boleto
 *       marcado como pago com a data do pagamento;</li>
 *   <li>Sincronização idempotente → sem save quando o boleto já está
 *       coerente com a conta.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PayableServiceTest {

    @Mock
    private PayableRepository repository;
    @Mock
    private PayableInstallmentRepository installmentRepository;
    @Mock
    private PayablePaymentRepository paymentRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private PayablePaymentAttachmentService paymentAttachmentService;
    @Mock
    private BoletoRepository boletoRepository;

    @InjectMocks
    private PayableService service;

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private static Payable payable(Long id, PayableSource source, Long boletoId, PayableStatus status,
                                   String value) {
        Payable p = new Payable();
        p.setId(id);
        p.setDescription("Fornecedor X - NF 100");
        p.setValue(new BigDecimal(value));
        p.setStatus(status);
        p.setSourceType(source);
        p.setBoletoId(boletoId);
        p.setSupplierId(5L);
        return p;
    }

    private static PayableInstallment installment(Long id, Long payableId, String amount,
                                                  PayableStatus status) {
        PayableInstallment inst = new PayableInstallment();
        inst.setId(id);
        inst.setPayableId(payableId);
        inst.setInstallmentNumber(1);
        inst.setAmount(new BigDecimal(amount));
        inst.setPaidAmount(BigDecimal.ZERO);
        inst.setStatus(status);
        return inst;
    }

    private static PayablePayment payment(Long id, Long payableId, Long installmentId,
                                          String amount, String paymentDate) {
        PayablePayment payment = new PayablePayment();
        payment.setId(id);
        payment.setPayableId(payableId);
        payment.setInstallmentId(installmentId);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentDate(LocalDate.parse(paymentDate));
        return payment;
    }

    private static Boleto boleto(Long id, boolean paid, String paymentDate) {
        Boleto b = new Boleto();
        b.setId(id);
        b.setValue(new BigDecimal("500.00"));
        b.setPaid(paid);
        b.setPaymentDate((paymentDate != null) ? LocalDate.parse(paymentDate) : null);
        return b;
    }

    // ------------------------------------------------------------------
    // removePayment — estorno
    // ------------------------------------------------------------------

    @Test
    void removePayment_contaBoletoQuitada_boletoDesmarcado() {
        Payable p = payable(10L, PayableSource.BOLETO, 7L, PayableStatus.PAGO, "500.00");
        PayableInstallment inst = installment(100L, 10L, "500.00", PayableStatus.PAGO);
        PayablePayment pay = payment(900L, 10L, 100L, "500.00", "2026-01-10");
        Boleto b = boleto(7L, true, "2026-01-10");

        when(repository.findById(10L)).thenReturn(Optional.of(p));
        when(paymentRepository.findByIdAndPayableId(900L, 10L)).thenReturn(Optional.of(pay));
        when(installmentRepository.findById(100L)).thenReturn(Optional.of(inst));
        when(installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(10L))
                .thenReturn(List.of(inst));
        when(boletoRepository.findById(7L)).thenReturn(Optional.of(b));
        when(repository.save(p)).thenReturn(p);

        service.removePayment(10L, 900L);

        // Conta e parcela voltam para ABERTO (saldo devedor restaurado).
        assertEquals(PayableStatus.ABERTO, p.getStatus());
        assertFalse(b.isPaid());
        assertNull(b.getPaymentDate());
        verify(boletoRepository).save(b);
    }

    @Test
    void removePayment_contaManual_naoTocaBoleto() {
        Payable p = payable(11L, PayableSource.MANUAL, null, PayableStatus.ABERTO, "300.00");
        PayableInstallment inst = installment(110L, 11L, "300.00", PayableStatus.PAGO);
        PayablePayment pay = payment(910L, 11L, 110L, "300.00", "2026-02-01");

        when(repository.findById(11L)).thenReturn(Optional.of(p));
        when(paymentRepository.findByIdAndPayableId(910L, 11L)).thenReturn(Optional.of(pay));
        when(installmentRepository.findById(110L)).thenReturn(Optional.of(inst));
        when(installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(11L))
                .thenReturn(List.of(inst));
        when(repository.save(p)).thenReturn(p);

        service.removePayment(11L, 910L);

        verify(boletoRepository, never()).findById(any());
        verify(boletoRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // registerPayment — pagamento direto pela conta
    // ------------------------------------------------------------------

    @Test
    void registerPayment_quitaContaBoleto_boletoMarcadoComoPago() {
        Payable p = payable(20L, PayableSource.BOLETO, 8L, PayableStatus.ABERTO, "300.00");
        PayableInstallment inst = installment(200L, 20L, "300.00", PayableStatus.ABERTO);
        Boleto b = boleto(8L, false, null);
        PayablePaymentRequest request =
                new PayablePaymentRequest(new BigDecimal("300.00"), LocalDate.parse("2026-01-15"), "TED");

        when(repository.findById(20L)).thenReturn(Optional.of(p));
        when(installmentRepository.findByIdAndPayableId(200L, 20L)).thenReturn(Optional.of(inst));
        when(paymentRepository.sumAmountByInstallmentId(200L)).thenReturn(new BigDecimal("300.00"));
        when(paymentRepository.findByInstallmentIdOrderByPaymentDateAsc(200L))
                .thenReturn(List.of(payment(950L, 20L, 200L, "300.00", "2026-01-15")));
        when(paymentRepository.sumAmountByPayableId(20L)).thenReturn(new BigDecimal("300.00"));
        when(installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(20L))
                .thenReturn(List.of(inst));
        when(boletoRepository.findById(8L)).thenReturn(Optional.of(b));
        when(repository.save(p)).thenReturn(p);

        service.registerPayment(20L, 200L, request);

        assertEquals(PayableStatus.PAGO, p.getStatus());
        ArgumentCaptor<Boleto> captor = ArgumentCaptor.forClass(Boleto.class);
        verify(boletoRepository).save(captor.capture());
        assertTrue(captor.getValue().isPaid());
        assertEquals(LocalDate.parse("2026-01-15"), captor.getValue().getPaymentDate());
    }

    @Test
    void syncBoletoState_boletoJaCoerente_naoSalva() {
        Payable p = payable(20L, PayableSource.BOLETO, 8L, PayableStatus.ABERTO, "300.00");
        PayableInstallment inst = installment(200L, 20L, "300.00", PayableStatus.ABERTO);
        Boleto b = boleto(8L, false, null);
        PayablePaymentRequest request =
                new PayablePaymentRequest(new BigDecimal("100.00"), LocalDate.parse("2026-01-15"), "TED");

        when(repository.findById(20L)).thenReturn(Optional.of(p));
        when(installmentRepository.findByIdAndPayableId(200L, 20L)).thenReturn(Optional.of(inst));
        when(paymentRepository.sumAmountByInstallmentId(200L)).thenReturn(new BigDecimal("100.00"));
        when(paymentRepository.findByInstallmentIdOrderByPaymentDateAsc(200L))
                .thenReturn(List.of(payment(950L, 20L, 200L, "100.00", "2026-01-15")));
        when(paymentRepository.sumAmountByPayableId(20L)).thenReturn(new BigDecimal("100.00"));
        when(installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(20L))
                .thenReturn(List.of(inst));
        when(boletoRepository.findById(8L)).thenReturn(Optional.of(b));
        when(repository.save(p)).thenReturn(p);

        // Pagamento parcial: conta continua ABERTO e o boleto já estava
        // não pago — nada deve mudar nem ser salvo.
        service.registerPayment(20L, 200L, request);

        assertEquals(PayableStatus.ABERTO, p.getStatus());
        assertFalse(b.isPaid());
        verify(boletoRepository, never()).save(any());
    }

    /** Nome legível para evitar confusão entre o flag e as classes. */
    private static boolean boleanoPago(Boleto b) {
        return b.isPaid();
    }
}
