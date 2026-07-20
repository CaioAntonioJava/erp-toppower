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

/**
 * Conversões entre entidades do agregado {@code SalesOrder} e seus DTOs.
 *
 * <p>Inclui o cálculo do {@code totalPrice} de cada item (líquido,
 * considerando o desconto por item) que afeta o total final do
 * pedido.</p>
 *
 * <p>Na <b>criação/edição direta</b>, a margem de lucro opcional do
 * header ({@code profitMargin}) é aplicada item a item aqui: o
 * {@code baseUnitPrice} preserva o preço original (sem margem) e o
 * {@code unitPrice} reflete a majoração
 * {@code baseUnitPrice × (1 + profitMargin/100)}.</p>
 *
 * <p>Na conversão de uma {@code Quotation} para {@code SalesOrder}
 * ({@link #fromQuotationItem(QuotationItem, UUID, BigDecimal)}), os
 * preços dos itens já vêm com a margem de lucro embutida (o
 * {@code QuotationMapper} aplica a margem item a item no momento da
 * criação/atualização). Por isso, esta conversão apenas copia os
 * valores, sem aplicar fator adicional — e o header convertido fica
 * com {@code profitMargin} nulo.</p>
 */
public final class SalesOrderMapper {

    private SalesOrderMapper() {
    }

    // ---------------------------------------------------------------------
    // Item
    // ---------------------------------------------------------------------

    /**
     * Cria uma entidade {@link SalesOrderItem} a partir do DTO de request,
     * aplicando a margem de lucro (quando informada) sobre o
     * {@code unitPrice} e calculando o {@code totalPrice} como
     * {@code (unitPrice × (1 + profitMargin/100)) × quantity − discount}
     * (com o desconto interpretado conforme {@code discountType}).
     *
     * @param request       DTO da linha
     * @param salesOrderUuid UUID do pedido
     * @param profitMargin  margem de lucro opcional; nula ou zero não aplica acréscimo
     */
    public static SalesOrderItem toItemEntity(SalesOrderItemRequest request, Long salesOrderId,
                                               BigDecimal profitMargin) {
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(salesOrderId);
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        // Preço base (sem margem) — persistido para que a edição do
        // pedido não reaplique a margem sobre o snapshot.
        item.setBaseUnitPrice(request.unitPrice());
        // Snapshot final: baseUnitPrice × (1 + profitMargin/100).
        item.setUnitPrice(applyProfitMargin(request.unitPrice(), profitMargin));
        item.setDiscountType(request.discountType());
        item.setDiscount(request.discount());
        item.setTotalPrice(calculateItemTotalPrice(
                request.unitPrice(),
                request.quantity(),
                request.discount(),
                request.discountType(),
                profitMargin));
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
    public static SalesOrderItem fromQuotationItem(QuotationItem source, Long salesOrderId,
                                                    BigDecimal profitMargin) {
        // no-op: profit margin already embedded in QuotationItem.unitPrice/totalPrice
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(salesOrderId);
        item.setProductId(source.getProductId());
        item.setQuantity(source.getQuantity());
        item.setUnitPrice(source.getUnitPrice());
        // Preserva o preço base (sem margem) vindo da cotação, para
        // rastreabilidade e eventual reversão pedido → cotação.
        item.setBaseUnitPrice(source.getBaseUnitPrice());
        item.setDiscountType(source.getDiscountType());
        item.setDiscount(source.getDiscount());
        item.setTotalPrice(source.getTotalPrice());
        return item;
    }

    public static SalesOrderItemResponse toItemResponse(SalesOrderItem item) {
        return new SalesOrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getBaseUnitPrice(),
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
        return calculateItemTotalPrice(unitPrice, quantity, discount, discountType, null);
    }

    /**
     * Calcula o total líquido de uma linha, aplicando a margem de lucro
     * <b>antes</b> do desconto da própria linha (espelha
     * {@code QuotationMapper.calculateItemTotalPrice}):
     * <pre>
     *   unitPriceComMargem = unitPrice × (1 + profitMargin/100)
     *   gross              = unitPriceComMargem × quantity
     *   desconto           = gross × discount%        (se PERCENT)
     *                      | discount (R$ fixo)       (se AMOUNT)
     *   totalPrice         = gross − desconto
     * </pre>
     */
    static BigDecimal calculateItemTotalPrice(BigDecimal unitPrice,
                                              BigDecimal quantity,
                                              BigDecimal discount,
                                              DiscountType discountType,
                                              BigDecimal profitMargin) {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal unitPriceWithMargin = applyProfitMargin(unitPrice, profitMargin);
        BigDecimal gross = unitPriceWithMargin.multiply(quantity);
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

    /**
     * Aplica a margem de lucro como multiplicação percentual sobre o
     * {@code unitPrice}, através do fator {@code (1 + profitMargin / 100)}.
     * Retorna {@code ZERO} quando o preço for nulo e o próprio preço
     * (arredondado para 2 casas) quando a margem for nula ou zero.
     */
    static BigDecimal applyProfitMargin(BigDecimal unitPrice, BigDecimal profitMargin) {
        if (unitPrice == null) {
            return BigDecimal.ZERO;
        }
        if (profitMargin == null || profitMargin.signum() == 0) {
            return unitPrice.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(
                profitMargin.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return unitPrice.multiply(factor).setScale(2, RoundingMode.HALF_UP);
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
        applyHeader(o, request.customerId(), request.companyId(), request.attention(),
                request.sellerId(), request.discountType(), request.discount(),
                request.paymentCondition(), request.notes(),
                request.freightType(), request.freightValue(), request.carrierId());
        o.setProfitMargin(request.profitMargin());
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
        o.setQuotationId(source.getId());
        o.setQuotationNumber(source.getNumber());

        String attention = (override != null && override.attention() != null)
                ? override.attention() : source.getAttention();
        PaymentCondition payment = (override != null && override.paymentCondition() != null)
                ? override.paymentCondition() : source.getPaymentCondition();
        String notes = (override != null && override.notes() != null)
                ? override.notes() : source.getNotes();

        applyHeader(o, source.getCustomerId(), source.getCompanyId(), attention,
                source.getSellerId(), source.getDiscountType(), source.getDiscount(),
                payment, notes,
                source.getFreightType(), source.getFreightValue(),
                source.getCarrierId());
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
        if (request.customerId() != null) {
            order.setCustomerId(request.customerId());
        }
        if (request.companyId() != null) {
            order.setCompanyId(request.companyId());
        }
        if (request.attention() != null) {
            order.setAttention(request.attention());
        }
        if (request.sellerId() != null) {
            order.setSellerId(request.sellerId());
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
        if (request.profitMargin() != null) {
            order.setProfitMargin(request.profitMargin());
        }
        // carrierId admite null (remoção da transportadora vinculada).
        order.setCarrierId(request.carrierId());
    }

    private static void applyHeader(SalesOrder o, Long customerId, Long companyId,
                                    String attention, Long sellerId,
                                    DiscountType discountType, BigDecimal discount,
                                    PaymentCondition paymentCondition, String notes,
                                    FreightType freightType,
                                    BigDecimal freightValue, Long carrierId) {
        o.setCustomerId(customerId);
        o.setCompanyId(companyId);
        o.setAttention(attention);
        o.setSellerId(sellerId);
        o.setDiscountType(discountType);
        o.setDiscount(discount);
        o.setPaymentCondition(paymentCondition);
        o.setNotes(notes);
        o.setFreightType(freightType);
        o.setFreightValue(freightValue);
        o.setCarrierId(carrierId);
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
                (order.getCustomerId() != null)
                        ? SalesOrderResponse.ClientType.CUSTOMER
                        : SalesOrderResponse.ClientType.COMPANY;

        return new SalesOrderResponse(
                order.getId(),
                order.getPrefix(),
                order.getSequence(),
                order.getYear(),
                order.formattedCode(),
                order.getOrderDate(),
                order.getCustomerId(),
                order.getCompanyId(),
                clientType,
                clientName,
                clientCode,
                order.getAttention(),
                order.getSellerId(),
                sellerName,
                items.stream().map(SalesOrderMapper::toItemResponse).toList(),
                order.getDiscountType(),
                order.getDiscount(),
                order.getPaymentCondition(),
                order.getNotes(),
                order.getFreightType(),
                order.getFreightValue(),
                order.getCarrierId(),
                carrierName,
                order.getStatus(),
                order.getQuotationId(),
                order.getQuotationNumber(),
                order.getProfitMargin(),
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
                (order.getCustomerId() != null)
                        ? SalesOrderResponse.ClientType.CUSTOMER
                        : SalesOrderResponse.ClientType.COMPANY;
        Long clientId = (order.getCustomerId() != null)
                ? order.getCustomerId()
                : order.getCompanyId();

        return new SalesOrderSummaryResponse(
                order.getId(),
                order.getPrefix(),
                order.getSequence(),
                order.getYear(),
                order.formattedCode(),
                order.getOrderDate(),
                clientType,
                clientId,
                clientName,
                clientCode,
                order.getSellerId(),
                sellerName,
                order.getStatus(),
                order.getTotalQuantity(),
                order.getTotal(),
                order.getPaymentCondition(),
                order.getQuotationNumber());
    }
}