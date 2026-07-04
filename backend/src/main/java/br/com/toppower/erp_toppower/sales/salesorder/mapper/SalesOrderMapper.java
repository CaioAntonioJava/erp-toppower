package br.com.toppower.erp_toppower.sales.salesorder.mapper;

import br.com.toppower.erp_toppower.sales.quotation.entity.Quotation;
import br.com.toppower.erp_toppower.sales.quotation.entity.QuotationItem;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderCreateRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderFromQuotationRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderItemRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderItemResponse;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderResponse;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderSummaryResponse;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderUpdateRequest;
import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrder;
import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Conversões entre entidades do agregado {@code SalesOrder} e seus DTOs.
 *
 * <p>Inclui o cálculo do {@code totalPrice} de cada item (líquido,
 * considerando o desconto por item) que afeta o total final do
 * pedido.</p>
 *
 * <p><b>Não transporta margem de lucro</b> da {@code Quotation} para o
 * {@code SalesOrder} — o pedido é o documento externo enviado ao
 * cliente, e a margem é informação interna mantida apenas na proposta.</p>
 */
public final class SalesOrderMapper {

    private SalesOrderMapper() {
    }

    // ---------------------------------------------------------------------
    // Item
    // ---------------------------------------------------------------------

    /**
     * Cria uma entidade {@link SalesOrderItem} a partir do DTO de request,
     * calculando o {@code totalPrice} como {@code unitPrice * quantity - discount}
     * (com o desconto interpretado conforme {@code discountType}).
     */
    public static SalesOrderItem toItemEntity(SalesOrderItemRequest request, UUID salesOrderUuid) {
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderUuid(salesOrderUuid);
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
     * Cria uma entidade {@link SalesOrderItem} a partir de um item de
     * proposta (snapshot na conversão). Copia produto, quantidade,
     * preço, desconto e total líquido — <b>não</b> copia margem.
     */
    public static SalesOrderItem fromQuotationItem(QuotationItem source, UUID salesOrderUuid) {
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderUuid(salesOrderUuid);
        item.setProductUuid(source.getProductUuid());
        item.setQuantity(source.getQuantity());
        item.setUnitPrice(source.getUnitPrice());
        item.setDiscountType(source.getDiscountType());
        item.setDiscount(source.getDiscount());
        item.setTotalPrice(source.getTotalPrice());
        return item;
    }

    public static SalesOrderItemResponse toItemResponse(SalesOrderItem item) {
        return new SalesOrderItemResponse(
                item.getUuid(),
                item.getProductUuid(),
                item.getQuantity(),
                item.getUnitPrice(),
                lineSubtotal(item),
                item.getDiscountType(),
                item.getDiscount(),
                item.getTotalPrice());
    }

    private static BigDecimal lineSubtotal(SalesOrderItem item) {
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
    // SalesOrder
    // ---------------------------------------------------------------------

    /**
     * Cria a entidade {@link SalesOrder} (header) a partir do request de
     * criação direta. O número e o status NÃO são definidos aqui — número
     * é gerado pelo service e status recebe o default {@code ABERTO} no
     * {@code @PrePersist}.
     */
    public static SalesOrder toEntity(SalesOrderCreateRequest request) {
        SalesOrder o = new SalesOrder();
        applyHeader(o, request.customerUuid(), request.companyUuid(), request.attention(),
                request.sellerUuid(), request.discountType(), request.discount(),
                request.paymentCondition(), request.notes(),
                request.carrierUuid(), request.freightType(), request.freightValue());
        return o;
    }

    /**
     * Cria a entidade {@link SalesOrder} (header) a partir de uma
     * {@link Quotation} convertida. Copia cliente, vendedor, descontos,
     * frete, condição de pagamento e observações; preenche
     * {@code quotationUuid} e {@code quotationNumber} para
     * rastreabilidade. Aplica sobrescritas opcionais vindas do request
     * de conversão. <b>Não copia margem de lucro</b>.
     */
    public static SalesOrder fromQuotation(Quotation source,
                                            List<QuotationItem> sourceItems,
                                            SalesOrderFromQuotationRequest override) {
        SalesOrder o = new SalesOrder();
        o.setQuotationUuid(source.getUuid());
        o.setQuotationNumber(source.getNumber());

        String attention = (override != null && override.attention() != null)
                ? override.attention() : source.getAttention();
        PaymentCondition payment = (override != null && override.paymentCondition() != null)
                ? override.paymentCondition() : source.getPaymentCondition();
        String notes = (override != null && override.notes() != null)
                ? override.notes() : source.getNotes();

        applyHeader(o, source.getCustomerUuid(), source.getCompanyUuid(), attention,
                source.getSellerUuid(), source.getDiscountType(), source.getDiscount(),
                payment, notes,
                source.getCarrierUuid(), source.getFreightType(), source.getFreightValue());
        return o;
    }

    /**
     * Aplica uma atualização parcial (PATCH) sobre a entidade carregada.
     * Apenas os campos não nulos do request sobrescrevem o estado atual.
     *
     * <p>A lista de itens é tratada como substituição completa — quando
     * informada, os itens anteriores são removidos (caller responsável)
     * e os novos itens são inseridos.</p>
     */
    public static void applyUpdate(SalesOrder order, SalesOrderUpdateRequest request) {
        if (request.customerUuid() != null) {
            order.setCustomerUuid(request.customerUuid());
        }
        if (request.companyUuid() != null) {
            order.setCompanyUuid(request.companyUuid());
        }
        if (request.attention() != null) {
            order.setAttention(request.attention());
        }
        if (request.sellerUuid() != null) {
            order.setSellerUuid(request.sellerUuid());
        }
        if (request.discountType() != null) {
            order.setDiscountType(request.discountType());
        }
        if (request.discount() != null) {
            order.setDiscount(request.discount());
        }
        if (request.paymentCondition() != null) {
            order.setPaymentCondition(request.paymentCondition());
        }
        if (request.notes() != null) {
            order.setNotes(request.notes());
        }
        if (request.carrierUuid() != null) {
            order.setCarrierUuid(request.carrierUuid());
        }
        if (request.freightType() != null) {
            order.setFreightType(request.freightType());
        }
        if (request.freightValue() != null) {
            order.setFreightValue(request.freightValue());
        }
    }

    private static void applyHeader(SalesOrder o, UUID customerUuid, UUID companyUuid,
                                    String attention, UUID sellerUuid,
                                    DiscountType discountType, BigDecimal discount,
                                    PaymentCondition paymentCondition, String notes,
                                    UUID carrierUuid, FreightType freightType,
                                    BigDecimal freightValue) {
        o.setCustomerUuid(customerUuid);
        o.setCompanyUuid(companyUuid);
        o.setAttention(attention);
        o.setSellerUuid(sellerUuid);
        o.setDiscountType(discountType);
        o.setDiscount(discount);
        o.setPaymentCondition(paymentCondition);
        o.setNotes(notes);
        o.setCarrierUuid(carrierUuid);
        o.setFreightType(freightType);
        o.setFreightValue(freightValue);
    }

    /**
     * Constrói a resposta completa a partir da entidade já com
     * totais calculados (via {@code recalculateTotals}) e da lista
     * de itens.
     */
    public static SalesOrderResponse toResponse(SalesOrder order, List<SalesOrderItem> items) {
        SalesOrderResponse.ClientType clientType =
                (order.getCustomerUuid() != null)
                        ? SalesOrderResponse.ClientType.CUSTOMER
                        : SalesOrderResponse.ClientType.COMPANY;

        return new SalesOrderResponse(
                order.getUuid(),
                order.getNumber(),
                order.getOrderDate(),
                order.getCustomerUuid(),
                order.getCompanyUuid(),
                clientType,
                order.getAttention(),
                order.getSellerUuid(),
                items.stream().map(SalesOrderMapper::toItemResponse).toList(),
                order.getDiscountType(),
                order.getDiscount(),
                order.getPaymentCondition(),
                order.getNotes(),
                order.getCarrierUuid(),
                order.getFreightType(),
                order.getFreightValue(),
                order.getStatus(),
                order.getQuotationUuid(),
                order.getQuotationNumber(),
                order.getSubtotal(),
                order.getTotal(),
                order.calculateGlobalDiscountValue(),
                order.getTotalQuantity(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCreatedBy(),
                order.getUpdatedBy());
    }

    /**
     * Constrói o resumo a partir da entidade. O nome do cliente é
     * resolvido pelo service e injetado neste ponto.
     */
    public static SalesOrderSummaryResponse toSummary(SalesOrder order, String clientName) {
        SalesOrderResponse.ClientType clientType =
                (order.getCustomerUuid() != null)
                        ? SalesOrderResponse.ClientType.CUSTOMER
                        : SalesOrderResponse.ClientType.COMPANY;
        UUID clientUuid = (order.getCustomerUuid() != null)
                ? order.getCustomerUuid()
                : order.getCompanyUuid();

        return new SalesOrderSummaryResponse(
                order.getUuid(),
                order.getNumber(),
                order.getOrderDate(),
                clientType,
                clientUuid,
                clientName,
                order.getSellerUuid(),
                order.getStatus(),
                order.getTotalQuantity(),
                order.getTotal(),
                order.getPaymentCondition(),
                order.getQuotationNumber());
    }
}