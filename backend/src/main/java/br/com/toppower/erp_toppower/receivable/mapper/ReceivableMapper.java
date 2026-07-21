package br.com.toppower.erp_toppower.receivable.mapper;

import br.com.toppower.erp_toppower.receivable.dto.ReceivableCreateRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableInstallmentRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableInstallmentResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivablePaymentRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivablePaymentResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableSummaryResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableUpdateRequest;
import br.com.toppower.erp_toppower.receivable.entity.Receivable;
import br.com.toppower.erp_toppower.receivable.entity.ReceivableInstallment;
import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mapper estático entre DTOs e entidades do módulo receivable.
 * Segue a convenção do projeto (sem MapStruct).
 */
public final class ReceivableMapper {

    private ReceivableMapper() {
    }

    // ---------------------------------------------------------------------
    // Create (manual)
    // ---------------------------------------------------------------------

    /**
     * Cria uma nova entidade a partir do request de criação manual.
     * A origem é sempre {@link ReceivableSource#MANUAL}; o
     * {@code @PrePersist} da entidade cuida de aplicar os defaults
     * {@code status = ABERTO}, {@code paidAmount = ZERO} e
     * {@code installmentsCount = 1}.
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
    public static List<ReceivableInstallment> toInstallments(Long receivableId,
                                                             ReceivableCreateRequest request) {
        List<ReceivableInstallmentRequest> raw = request.installments();
        if (raw == null || raw.isEmpty()) {
            ReceivableInstallment single = new ReceivableInstallment();
            single.setReceivableId(receivableId);
            single.setInstallmentNumber(1);
            single.setAmount(request.value());
            single.setDueDate(request.dueDate());
            single.setPaidAmount(BigDecimal.ZERO);
            single.setStatus(ReceivableStatus.ABERTO);
            return List.of(single);
        }
        List<ReceivableInstallment> result = new ArrayList<>(raw.size());
        int n = 1;
        for (ReceivableInstallmentRequest r : raw) {
            ReceivableInstallment inst = new ReceivableInstallment();
            inst.setReceivableId(receivableId);
            inst.setInstallmentNumber(n++);
            inst.setAmount(r.amount());
            inst.setDueDate(r.dueDate());
            inst.setPaidAmount(BigDecimal.ZERO);
            inst.setStatus(ReceivableStatus.ABERTO);
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

    // ---------------------------------------------------------------------
    // Payments
    // ---------------------------------------------------------------------

    public static ReceivablePayment toPaymentEntity(Long receivableId,
                                                    Long installmentId,
                                                    ReceivablePaymentRequest request) {
        ReceivablePayment p = new ReceivablePayment();
        p.setReceivableId(receivableId);
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
     * histórico de pagamentos. Recebe o nome/código do cliente já
     * resolvidos pelo service.
     */
    public static ReceivableResponse toResponse(Receivable r,
                                                String clientName,
                                                String clientCode,
                                                List<ReceivableInstallment> installments,
                                                List<ReceivablePayment> payments) {
        BigDecimal paidAmount = (r.getPaidAmount() != null) ? r.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = r.getValue().subtract(paidAmount);
        List<ReceivableInstallmentResponse> installmentResponses = (installments == null)
                ? List.of()
                : installments.stream().map(ReceivableMapper::toInstallmentResponse).toList();

        // Pré-cria um mapa installmentId -> installmentNumber para evitar
        // busca linear ao montar cada pagamento.
        Map<Long, ReceivableInstallment> installmentById = (installments == null)
                ? Map.of()
                : installments.stream().collect(Collectors.toMap(
                        ReceivableInstallment::getId, Function.identity()));
        List<ReceivablePaymentResponse> paymentResponses = (payments == null)
                ? List.of()
                : payments.stream()
                        .map(pay -> toPaymentResponse(pay, installmentById))
                        .toList();

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
                r.getInstallmentsCount(),
                r.getSalesOrderId(),
                r.getSalesOrderNumber(),
                r.getSalesOrderCode(),
                r.getTechnicalProposalId(),
                r.getTechnicalProposalCode(),
                r.getContractId(),
                r.getContractCode(),
                r.getPaymentDate(),
                installmentResponses,
                paymentResponses,
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getCreatedBy(),
                r.getUpdatedBy()
        );
    }

    /**
     * Monta o resumo para listas paginadas (sem parcelas nem
     * pagamentos).
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
                sourceCodeOf(r),
                clientName,
                clientCode,
                r.getInstallmentsCount(),
                r.getPaymentDate()
        );
    }

    public static ReceivableInstallmentResponse toInstallmentResponse(ReceivableInstallment i) {
        BigDecimal paidAmount = (i.getPaidAmount() != null) ? i.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = i.getAmount().subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);
        return new ReceivableInstallmentResponse(
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

    public static ReceivablePaymentResponse toPaymentResponse(ReceivablePayment p,
                                                              Map<Long, ReceivableInstallment> installmentById) {
        int installmentNumber = 0;
        if (installmentById != null && p.getInstallmentId() != null) {
            ReceivableInstallment inst = installmentById.get(p.getInstallmentId());
            if (inst != null) {
                installmentNumber = inst.getInstallmentNumber();
            }
        }
        return new ReceivablePaymentResponse(
                p.getId(),
                p.getInstallmentId(),
                installmentNumber,
                p.getAmount(),
                p.getPaymentDate(),
                p.getNotes(),
                p.getCreatedAt()
        );
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Resolve o código do documento de origem conforme {@link ReceivableSource}.
     * Para {@link ReceivableSource#MANUAL} retorna {@code null} (não há
     * documento de origem); para {@link ReceivableSource#SALES_ORDER} retorna
     * o código formatado do pedido (snapshot {@code salesOrderCode}),
     * com fallback para {@code salesOrderNumber} em contas antigas pré-refatoração;
     * para proposta técnica e contrato, retorna o código formatado persistido
     * no snapshot.
     */
    private static String sourceCodeOf(Receivable r) {
        if (r.getSourceType() == null) {
            return null;
        }
        return switch (r.getSourceType()) {
            case SALES_ORDER -> (r.getSalesOrderCode() != null)
                    ? r.getSalesOrderCode()
                    : (r.getSalesOrderNumber() != null)
                            ? r.getSalesOrderNumber().toString()
                            : null;
            case TECHNICAL_PROPOSAL -> r.getTechnicalProposalCode();
            case CONTRACT -> r.getContractCode();
            case MANUAL -> null;
        };
    }
}