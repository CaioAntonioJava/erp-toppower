package br.com.toppower.erp_toppower.receivable.mapper;

import br.com.toppower.erp_toppower.receivable.dto.ReceivableCreateRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivablePaymentResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableSummaryResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableUpdateRequest;
import br.com.toppower.erp_toppower.receivable.entity.Receivable;
import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper estático entre DTOs e entidades do módulo receivable.
 * Segue a convenção do projeto (sem MapStruct).
 */
public final class ReceivableMapper {

    private ReceivableMapper() {
    }

    /**
     * Cria uma nova entidade a partir do request de criação manual.
     * A origem é sempre {@link ReceivableSource#MANUAL}; o
     * {@code @PrePersist} da entidade cuida de aplicar os defaults
     * {@code status = ABERTO} e {@code paidAmount = ZERO}.
     */
    public static Receivable toEntity(ReceivableCreateRequest request) {
        Receivable r = new Receivable();
        r.setDescription(request.description());
        r.setValue(request.value());
        r.setDueDate(request.dueDate());
        r.setCustomerId(request.customerId());
        r.setCompanyId(request.companyId());
        r.setPaymentCondition(request.paymentCondition());
        r.setSourceType(ReceivableSource.MANUAL);
        return r;
    }

    /**
     * Monta a resposta completa, incluindo o histórico de pagamentos e o
     * saldo devedor calculado.
     */
    public static ReceivableResponse toResponse(Receivable r,
                                                String clientName,
                                                String clientCode,
                                                List<ReceivablePayment> payments) {
        BigDecimal paidAmount = (r.getPaidAmount() != null) ? r.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = r.getValue().subtract(paidAmount);
        List<ReceivablePaymentResponse> paymentResponses = (payments == null)
                ? List.of()
                : payments.stream().map(ReceivableMapper::toPaymentResponse).toList();
        return new ReceivableResponse(
                r.getId(),
                r.getDescription(),
                r.getValue(),
                paidAmount,
                balance,
                r.getDueDate(),
                r.getStatus(),
                r.getSourceType(),
                r.getCustomerId(),
                r.getCompanyId(),
                clientName,
                clientCode,
                r.getPaymentCondition(),
                r.getSalesOrderId(),
                r.getSalesOrderNumber(),
                r.getTechnicalProposalId(),
                r.getTechnicalProposalCode(),
                r.getContractId(),
                r.getContractCode(),
                r.getPaymentDate(),
                paymentResponses,
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getCreatedBy(),
                r.getUpdatedBy()
        );
    }

    /**
     * Monta o resumo para listas paginadas (sem histórico de pagamentos).
     */
    public static ReceivableSummaryResponse toSummary(Receivable r,
                                                     String clientName,
                                                     String clientCode) {
        BigDecimal paidAmount = (r.getPaidAmount() != null) ? r.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = r.getValue().subtract(paidAmount);
        return new ReceivableSummaryResponse(
                r.getId(),
                r.getDescription(),
                r.getValue(),
                paidAmount,
                balance,
                r.getDueDate(),
                r.getStatus(),
                r.getSourceType(),
                clientName,
                clientCode,
                r.getPaymentDate()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     * Valor, origem e vínculos com documentos de origem não são editáveis.
     */
    public static void applyUpdate(Receivable r, ReceivableUpdateRequest request) {
        if (request.description() != null) {
            r.setDescription(request.description());
        }
        if (request.dueDate() != null) {
            r.setDueDate(request.dueDate());
        }
        if (request.paymentCondition() != null) {
            r.setPaymentCondition(request.paymentCondition());
        }
    }

    public static ReceivablePayment toPaymentEntity(Long receivableId,
                                                    br.com.toppower.erp_toppower.receivable.dto.ReceivablePaymentRequest request) {
        ReceivablePayment p = new ReceivablePayment();
        p.setReceivableId(receivableId);
        p.setAmount(request.amount());
        p.setPaymentDate(request.paymentDate());
        p.setNotes(request.notes());
        return p;
    }

    public static ReceivablePaymentResponse toPaymentResponse(ReceivablePayment p) {
        return new ReceivablePaymentResponse(
                p.getId(),
                p.getAmount(),
                p.getPaymentDate(),
                p.getNotes(),
                p.getCreatedAt()
        );
    }
}