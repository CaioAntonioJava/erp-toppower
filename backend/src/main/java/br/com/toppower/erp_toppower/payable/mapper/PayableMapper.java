package br.com.toppower.erp_toppower.payable.mapper;

import br.com.toppower.erp_toppower.payable.dto.PayableCreateRequest;
import br.com.toppower.erp_toppower.payable.dto.PayableInstallmentRequest;
import br.com.toppower.erp_toppower.payable.dto.PayableInstallmentResponse;
import br.com.toppower.erp_toppower.payable.dto.PayablePaymentRequest;
import br.com.toppower.erp_toppower.payable.dto.PayablePaymentResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableSummaryResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableUpdateRequest;
import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.entity.PayableInstallment;
import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mapper estático entre DTOs e entidades do módulo payable.
 * Segue a convenção do projeto (sem MapStruct).
 */
public final class PayableMapper {

    private PayableMapper() {
    }

    // ---------------------------------------------------------------------
    // Create (manual)
    // ---------------------------------------------------------------------

    /**
     * Cria uma nova entidade a partir do request de criação manual.
     * A origem é sempre {@link PayableSource#MANUAL}; o
     * {@code @PrePersist} da entidade cuida de aplicar os defaults
     * {@code status = ABERTO}, {@code paidAmount = ZERO} e
     * {@code installmentsCount = 1}.
     */
    public static Payable toEntity(PayableCreateRequest request) {
        Payable p = new Payable();
        p.setDescription(request.description());
        p.setValue(request.value());
        p.setIssueDate(request.issueDate());
        p.setDueDate(request.dueDate());
        p.setSupplierId(request.supplierId());
        p.setPaymentCondition(request.paymentCondition());
        p.setSourceType(PayableSource.MANUAL);
        return p;
    }

    /**
     * Gera a lista de parcelas programadas a partir do request de
     * criação. Quando {@code request.installments()} é nulo ou vazio,
     * cria uma única parcela (à vista) com o valor total e o
     * vencimento-base. Caso contrário, usa as parcelas explícitas
     * (preservando valores e vencimentos informados pelo usuário).
     *
     * <p><b>Atenção</b>: o service valida que a soma das parcelas
     * explícitas bate com {@code value}. O mapper apenas materializa
     * o que foi informado — não recalcula nem redistribui valores.</p>
     */
    public static List<PayableInstallment> toInstallments(Long payableId,
                                                          PayableCreateRequest request) {
        List<PayableInstallmentRequest> raw = request.installments();
        if (raw == null || raw.isEmpty()) {
            PayableInstallment single = new PayableInstallment();
            single.setPayableId(payableId);
            single.setInstallmentNumber(1);
            single.setAmount(request.value());
            single.setDueDate(request.dueDate());
            single.setPaidAmount(BigDecimal.ZERO);
            single.setStatus(PayableStatus.ABERTO);
            return List.of(single);
        }
        List<PayableInstallment> result = new ArrayList<>(raw.size());
        int n = 1;
        for (PayableInstallmentRequest r : raw) {
            PayableInstallment inst = new PayableInstallment();
            inst.setPayableId(payableId);
            inst.setInstallmentNumber(n++);
            inst.setAmount(r.amount());
            inst.setDueDate(r.dueDate());
            inst.setPaidAmount(BigDecimal.ZERO);
            inst.setStatus(PayableStatus.ABERTO);
            result.add(inst);
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Update (PATCH)
    // ---------------------------------------------------------------------

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     * Valor, origem, fornecedor, parcelas e vínculos com documentos de
     * origem não são editáveis.
     */
    public static void applyUpdate(Payable p, PayableUpdateRequest request) {
        if (request.description() != null) {
            p.setDescription(request.description());
        }
        if (request.issueDate() != null) {
            p.setIssueDate(request.issueDate());
        }
        if (request.dueDate() != null) {
            p.setDueDate(request.dueDate());
        }
        if (request.paymentCondition() != null) {
            p.setPaymentCondition(request.paymentCondition());
        }
    }

    // ---------------------------------------------------------------------
    // Payments
    // ---------------------------------------------------------------------

    public static PayablePayment toPaymentEntity(Long payableId,
                                                 Long installmentId,
                                                 PayablePaymentRequest request) {
        PayablePayment p = new PayablePayment();
        p.setPayableId(payableId);
        p.setInstallmentId(installmentId);
        p.setAmount(request.amount());
        p.setPaymentDate(request.paymentDate());
        p.setNotes(request.notes());
        return p;
    }

    // ---------------------------------------------------------------------
    // Responses
    // ---------------------------------------------------------------------

    /**
     * Monta a resposta completa, incluindo parcelas programadas e
     * histórico de pagamentos. Recebe o nome/CNPJ do fornecedor já
     * resolvidos pelo service.
     */
    public static PayableResponse toResponse(Payable p,
                                             String supplierName,
                                             String supplierTaxId,
                                             List<PayableInstallment> installments,
                                             List<PayablePayment> payments) {
        BigDecimal paidAmount = (p.getPaidAmount() != null) ? p.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = p.getValue().subtract(paidAmount);
        List<PayableInstallmentResponse> installmentResponses = (installments == null)
                ? List.of()
                : installments.stream().map(PayableMapper::toInstallmentResponse).toList();

        // Pré-cria um mapa installmentId -> installmentNumber para evitar
        // busca linear ao montar cada pagamento.
        Map<Long, PayableInstallment> installmentById = (installments == null)
                ? Map.of()
                : installments.stream().collect(Collectors.toMap(
                        PayableInstallment::getId, Function.identity()));
        List<PayablePaymentResponse> paymentResponses = (payments == null)
                ? List.of()
                : payments.stream()
                        .map(pay -> toPaymentResponse(pay, installmentById))
                        .toList();

        return new PayableResponse(
                p.getId(),
                p.getDescription(),
                p.getValue(),
                paidAmount,
                balance,
                p.getIssueDate(),
                p.getDueDate(),
                p.getStatus(),
                p.getSourceType(),
                p.getSupplierId(),
                supplierName,
                supplierTaxId,
                p.getPaymentCondition(),
                p.getInstallmentsCount(),
                p.getBoletoId(),
                p.getPurchaseInvoiceId(),
                p.getPurchaseInvoiceNumber(),
                p.getPaymentDate(),
                installmentResponses,
                paymentResponses,
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getCreatedBy(),
                p.getUpdatedBy()
        );
    }

    /**
     * Monta o resumo para listas paginadas (sem parcelas nem
     * pagamentos).
     */
    public static PayableSummaryResponse toSummary(Payable p,
                                                    String supplierName,
                                                    String supplierTaxId) {
        BigDecimal paidAmount = (p.getPaidAmount() != null) ? p.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = p.getValue().subtract(paidAmount);
        return new PayableSummaryResponse(
                p.getId(),
                p.getDescription(),
                p.getValue(),
                paidAmount,
                balance,
                p.getIssueDate(),
                p.getDueDate(),
                p.getStatus(),
                p.getSourceType(),
                p.getSupplierId(),
                supplierName,
                supplierTaxId,
                p.getInstallmentsCount(),
                p.getPaymentDate()
        );
    }

    public static PayableInstallmentResponse toInstallmentResponse(PayableInstallment i) {
        BigDecimal paidAmount = (i.getPaidAmount() != null) ? i.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = i.getAmount().subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);
        return new PayableInstallmentResponse(
                i.getId(),
                i.getInstallmentNumber(),
                i.getAmount(),
                paidAmount,
                balance,
                i.getDueDate(),
                i.getStatus(),
                i.getPaymentDate()
        );
    }

    public static PayablePaymentResponse toPaymentResponse(PayablePayment p,
                                                           Map<Long, PayableInstallment> installmentById) {
        int installmentNumber = 0;
        if (installmentById != null && p.getInstallmentId() != null) {
            PayableInstallment inst = installmentById.get(p.getInstallmentId());
            if (inst != null) {
                installmentNumber = inst.getInstallmentNumber();
            }
        }
        return new PayablePaymentResponse(
                p.getId(),
                p.getInstallmentId(),
                installmentNumber,
                p.getAmount(),
                p.getPaymentDate(),
                p.getNotes(),
                p.getReceiptUrl(),
                p.getCreatedAt()
        );
    }
}