package br.com.toppower.erp_toppower.sales.quotation.mapper;

import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationCreateRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationItemRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationItemResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSimulateItemRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSimulateRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSimulateResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationUpdateRequest;
import br.com.toppower.erp_toppower.sales.quotation.entity.Quotation;
import br.com.toppower.erp_toppower.sales.quotation.entity.QuotationItem;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Conversões entre entidades do agregado {@code Quotation} e seus DTOs.
 *
 * <p>Inclui o cálculo do {@code totalPrice} de cada item (líquido,
 * considerando o desconto por item) que afeta o total final da
 * proposta.</p>
 */
public final class QuotationMapper {

    private QuotationMapper() {
    }

    // ---------------------------------------------------------------------
    // Item
    // ---------------------------------------------------------------------

    /**
     * Cria uma entidade {@link QuotationItem} a partir do DTO de request,
     * calculando o {@code totalPrice} como {@code unitPrice * quantity - discount}
     * (com o desconto interpretado conforme {@code discountType}).
     */
    public static QuotationItem toItemEntity(QuotationItemRequest request, UUID quotationUuid) {
        QuotationItem item = new QuotationItem();
        item.setQuotationUuid(quotationUuid);
        item.setProductUuid(request.productUuid());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setDiscountType(request.discountType());
        item.setDiscount(request.discount());
        item.setTotalPrice(calculateItemTotalPrice(
                request.unitPrice(),
                request.quantity(),
                request.discount(),
                request.discountType()));
        return item;
    }

    /**
     * Cria uma entidade {@link QuotationItem} a partir do DTO permissivo
     * de simulação. Campos nulos (quantidade/preço/desconto) são tratados
     * como zero pelo cálculo — o preview pode ser disparado com o
     * formulário em estado intermediário.
     */
    public static QuotationItem toItemEntity(QuotationSimulateItemRequest request, UUID quotationUuid) {
        QuotationItem item = new QuotationItem();
        item.setQuotationUuid(quotationUuid);
        item.setProductUuid(request.productUuid());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setDiscountType(request.discountType());
        item.setDiscount(request.discount());
        item.setTotalPrice(calculateItemTotalPrice(
                request.unitPrice(),
                request.quantity(),
                request.discount(),
                request.discountType()));
        return item;
    }

    public static QuotationItemResponse toItemResponse(QuotationItem item) {
        return new QuotationItemResponse(
                item.getUuid(),
                item.getProductUuid(),
                item.getQuantity(),
                item.getUnitPrice(),
                lineSubtotal(item),
                item.getDiscountType(),
                item.getDiscount(),
                item.getTotalPrice());
    }

    private static BigDecimal lineSubtotal(QuotationItem item) {
        if (item.getUnitPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return item.getUnitPrice().multiply(item.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o total líquido de uma linha:
     * {@code unitPrice * quantity} menos o desconto (valor fixo ou percentual).
     */
    static BigDecimal calculateItemTotalPrice(BigDecimal unitPrice,
                                               BigDecimal quantity,
                                               BigDecimal discount,
                                               DiscountType discountType) {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal gross = unitPrice.multiply(quantity);
        if (discount == null || discountType == null || discount.signum() == 0) {
            return gross.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal discountAmount = switch (discountType) {
            case AMOUNT -> discount;
            case PERCENT -> gross.multiply(discount)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        };
        BigDecimal net = gross.subtract(discountAmount);
        return net.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------------
    // Quotation
    // ---------------------------------------------------------------------

    /**
     * Cria a entidade {@link Quotation} (header) a partir do request de
     * criação. O número e o status NÃO são definidos aqui — número é
     * gerado pelo service e status recebe o default {@code ATIVA} no
     * {@code @PrePersist}.
     */
    public static Quotation toEntity(QuotationCreateRequest request) {
        Quotation q = new Quotation();
        applyHeader(q, request.customerUuid(), request.companyUuid(), request.attention(),
                request.sellerUuid(), request.discountType(), request.discount(),
                request.validityDays(), request.paymentCondition(), request.notes(),
                request.carrierUuid(), request.freightType(), request.freightValue(),
                request.profitMargin());
        return q;
    }

    /**
     * Cria a entidade {@link Quotation} (header) a partir do request
     * permissivo de simulação. Apenas os campos relevantes para o cálculo
     * são populados; campos nulos são tolerados (o cálculo trata como
     * zero). Não define número nem status — o simulate não persiste.
     */
    public static Quotation toEntity(QuotationSimulateRequest request) {
        Quotation q = new Quotation();
        applyHeader(q, request.customerUuid(), request.companyUuid(), request.attention(),
                request.sellerUuid(), request.discountType(), request.discount(),
                request.validityDays(), request.paymentCondition(), request.notes(),
                request.carrierUuid(), request.freightType(), request.freightValue(),
                request.profitMargin());
        return q;
    }

    /**
     * Aplica uma atualização parcial (PATCH) sobre a entidade carregada.
     * Apenas os campos não nulos do request sobrescrevem o estado atual.
     *
     * <p>A lista de itens é tratada como substituição completa — quando
     * informada, os itens anteriores são removidos (caller responsável)
     * e os novos itens são inseridos.</p>
     */
    public static void applyUpdate(Quotation quotation, QuotationUpdateRequest request) {
        if (request.customerUuid() != null) {
            quotation.setCustomerUuid(request.customerUuid());
        }
        if (request.companyUuid() != null) {
            quotation.setCompanyUuid(request.companyUuid());
        }
        if (request.attention() != null) {
            quotation.setAttention(request.attention());
        }
        if (request.sellerUuid() != null) {
            quotation.setSellerUuid(request.sellerUuid());
        }
        if (request.discountType() != null) {
            quotation.setDiscountType(request.discountType());
        }
        if (request.discount() != null) {
            quotation.setDiscount(request.discount());
        }
        if (request.validityDays() != null) {
            quotation.setValidityDays(request.validityDays());
        }
        if (request.paymentCondition() != null) {
            quotation.setPaymentCondition(request.paymentCondition());
        }
        if (request.notes() != null) {
            quotation.setNotes(request.notes());
        }
        if (request.carrierUuid() != null) {
            quotation.setCarrierUuid(request.carrierUuid());
        }
        if (request.freightType() != null) {
            quotation.setFreightType(request.freightType());
        }
        if (request.freightValue() != null) {
            quotation.setFreightValue(request.freightValue());
        }
        if (request.profitMargin() != null) {
            quotation.setProfitMargin(request.profitMargin());
        }
    }

    private static void applyHeader(Quotation q, UUID customerUuid, UUID companyUuid,
                                    String attention, UUID sellerUuid,
                                    DiscountType discountType, BigDecimal discount,
                                    Integer validityDays, br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition paymentCondition,
                                    String notes, UUID carrierUuid,
                                    FreightType freightType, BigDecimal freightValue,
                                    BigDecimal profitMargin) {
        q.setCustomerUuid(customerUuid);
        q.setCompanyUuid(companyUuid);
        q.setAttention(attention);
        q.setSellerUuid(sellerUuid);
        q.setDiscountType(discountType);
        q.setDiscount(discount);
        q.setValidityDays(validityDays);
        q.setPaymentCondition(paymentCondition);
        q.setNotes(notes);
        q.setCarrierUuid(carrierUuid);
        q.setFreightType(freightType);
        q.setFreightValue(freightValue);
        q.setProfitMargin(profitMargin);
    }

    /**
     * Constrói a resposta completa a partir da entidade já com
     * totais calculados (via {@code recalculateTotals}) e da lista
     * de itens.
     */
    public static QuotationResponse toResponse(Quotation quotation, List<QuotationItem> items) {
        QuotationResponse.ClientType clientType =
                (quotation.getCustomerUuid() != null)
                        ? QuotationResponse.ClientType.CUSTOMER
                        : QuotationResponse.ClientType.COMPANY;

        return new QuotationResponse(
                quotation.getUuid(),
                quotation.getNumber(),
                quotation.getIssueDate(),
                quotation.getCustomerUuid(),
                quotation.getCompanyUuid(),
                clientType,
                quotation.getAttention(),
                quotation.getSellerUuid(),
                items.stream().map(QuotationMapper::toItemResponse).toList(),
                quotation.getDiscountType(),
                quotation.getDiscount(),
                quotation.getValidityDays(),
                quotation.getPaymentCondition(),
                quotation.getNotes(),
                quotation.getCarrierUuid(),
                quotation.getFreightType(),
                quotation.getFreightValue(),
                quotation.getProfitMargin(),
                quotation.getStatus(),
                quotation.getSubtotal(),
                quotation.getTotal(),
                quotation.calculateGlobalDiscountValue(),
                quotation.getTotalQuantity(),
                quotation.getCreatedAt(),
                quotation.getUpdatedAt(),
                quotation.getCreatedBy(),
                quotation.getUpdatedBy());
    }

    /**
     * Constrói o resumo de simulação (totais sem persistência) a partir
     * da entidade já com totais calculados (via {@code recalculateTotals})
     * e da lista de itens.
     */
    public static QuotationSimulateResponse toSimulateResponse(Quotation quotation, List<QuotationItem> items) {
        return new QuotationSimulateResponse(
                items.stream().map(QuotationMapper::toItemResponse).toList(),
                quotation.getSubtotal(),
                quotation.calculateGlobalDiscountValue(),
                quotation.getTotal(),
                quotation.getTotalQuantity());
    }

    /**
     * Constrói o resumo a partir da entidade. O nome do cliente é
     * resolvido pelo service e injetado neste ponto.
     */
    public static QuotationSummaryResponse toSummary(Quotation quotation, String clientName) {
        QuotationResponse.ClientType clientType =
                (quotation.getCustomerUuid() != null)
                        ? QuotationResponse.ClientType.CUSTOMER
                        : QuotationResponse.ClientType.COMPANY;
        UUID clientUuid = (quotation.getCustomerUuid() != null)
                ? quotation.getCustomerUuid()
                : quotation.getCompanyUuid();

        return new QuotationSummaryResponse(
                quotation.getUuid(),
                quotation.getNumber(),
                quotation.getIssueDate(),
                clientType,
                clientUuid,
                clientName,
                quotation.getSellerUuid(),
                quotation.getStatus(),
                quotation.getTotalQuantity(),
                quotation.getTotal(),
                quotation.getPaymentCondition());
    }
}
