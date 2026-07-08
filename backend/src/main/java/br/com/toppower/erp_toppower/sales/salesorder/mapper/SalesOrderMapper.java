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
 * <p>Na conversão de uma {@code Quotation} para {@code SalesOrder}
 * ({@link #fromQuotationItem(QuotationItem, UUID, BigDecimal)}), os
 * preços dos itens já vêm com a margem de lucro embutida (o
 * {@code QuotationMapper} aplica a margem item a item no momento da
 * criação/atualização). Por isso, esta conversão apenas copia os
 * valores, sem aplicar fator adicional.</p>
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
     * proposta (snapshot na conversão). Copia produto, quantidade, tipo
     * de desconto, desconto e totais <b>sem aplicar fator de margem</b>:
     * a margem de lucro da proposta já está embutida em
     * {@code source.unitPrice} e {@code source.totalPrice}, calculada
     * item a item pelo {@code QuotationMapper} no momento da
     * criação/atualização da cotação.
     *
     * <p>O parâmetro {@code profitMargin} é mantido por compatibilidade
     * com {@code SalesOrderService.createFromQuotation}, mas é
     * ignorado neste método — a aplicação da margem é responsabilidade
     * exclusiva do mapper de cotação. Marcado como {@code @Deprecated}
     * para sinalizar que deve ser removido quando a assinatura puder
     * ser quebrada.</p>
     *
     * @param source       item da cotação já com margem embutida
     * @param salesOrderUuid UUID do pedido de venda
     * @param profitMargin ignorado — a margem já está no snapshot
     * @return item do pedido de venda
     */
    @Deprecated
    public static SalesOrderItem fromQuotationItem(QuotationItem source, UUID salesOrderUuid,
                                                    BigDecimal profitMargin) {
        // no-op: profit margin already embedded in QuotationItem.unitPrice/totalPrice
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
                request.freightType(), request.freightValue(), request.carrierUuid());
        return o;
    }

    /**
     * Cria a entidade {@link SalesOrder} (header) a partir de uma
     * {@link Quotation} convertida. Copia cliente, vendedor, descontos,
     * frete, condição de pagamento e observações; preenche
     * {@code quotationUuid} e {@code quotationNumber} para
     * rastreabilidade. Aplica sobrescritas opcionais vindas do request
     * de conversão. A margem de lucro é embutida nos preços dos itens
     * pelo mapper de itens, não no header.
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
                source.getFreightType(), source.getFreightValue(),
                source.getCarrierUuid());
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
        if (request.freightType() != null) {
            order.setFreightType(request.freightType());
        }
        if (request.freightValue() != null) {
            order.setFreightValue(request.freightValue());
        }
        // carrierUuid admite null (remoção da transportadora vinculada).
        order.setCarrierUuid(request.carrierUuid());
    }

    private static void applyHeader(SalesOrder o, UUID customerUuid, UUID companyUuid,
                                    String attention, UUID sellerUuid,
                                    DiscountType discountType, BigDecimal discount,
                                    PaymentCondition paymentCondition, String notes,
                                    FreightType freightType,
                                    BigDecimal freightValue, UUID carrierUuid) {
        o.setCustomerUuid(customerUuid);
        o.setCompanyUuid(companyUuid);
        o.setAttention(attention);
        o.setSellerUuid(sellerUuid);
        o.setDiscountType(discountType);
        o.setDiscount(discount);
        o.setPaymentCondition(paymentCondition);
        o.setNotes(notes);
        o.setFreightType(freightType);
        o.setFreightValue(freightValue);
        o.setCarrierUuid(carrierUuid);
    }

    /**
     * Constrói a resposta completa a partir da entidade já com
     * totais calculados (via {@code recalculateTotals}) e da lista
     * de itens. O nome do vendedor e os dados do cliente (nome e código)
     * são resolvidos pelo service e injetados aqui, evitando um
     * round-trip adicional no frontend (que antes exigia
     * ROLE_ADMIN/MANAGER para chamar {@code GET /sellers/{id}} e um
     * typeahead de query vazia para hidratar o cliente).
     */
    public static SalesOrderResponse toResponse(SalesOrder order, List<SalesOrderItem> items,
                                                String sellerName, String clientName, String clientCode,
                                                String carrierName) {
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
                clientName,
                clientCode,
                order.getAttention(),
                order.getSellerUuid(),
                sellerName,
                items.stream().map(SalesOrderMapper::toItemResponse).toList(),
                order.getDiscountType(),
                order.getDiscount(),
                order.getPaymentCondition(),
                order.getNotes(),
                order.getFreightType(),
                order.getFreightValue(),
                order.getCarrierUuid(),
                carrierName,
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
     * Constrói o resumo a partir da entidade. Os dados do cliente (nome e
     * código) e o nome do vendedor são resolvidos pelo service e injetados
     * neste ponto.
     */
    public static SalesOrderSummaryResponse toSummary(SalesOrder order, String clientName,
                                                      String clientCode, String sellerName) {
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
                clientCode,
                order.getSellerUuid(),
                sellerName,
                order.getStatus(),
                order.getTotalQuantity(),
                order.getTotal(),
                order.getPaymentCondition(),
                order.getQuotationNumber());
    }
}