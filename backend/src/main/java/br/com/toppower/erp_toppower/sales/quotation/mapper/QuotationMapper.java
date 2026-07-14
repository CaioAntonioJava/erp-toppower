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

/**
 * Conversões entre entidades do agregado {@code Quotation} e seus DTOs.
 *
 * <p>A margem de lucro ({@code profitMargin}) do header é aplicada
 * <b>item a item</b> aqui: o {@code unitPrice} persistido já reflete a
 * majoração e o {@code totalPrice} é calculado sobre esse preço
 * majorado, líquido do desconto da própria linha. Com isso, o total da
 * proposta é simplesmente a soma dos itens (já com margem e já líquido
 * do desconto por item), menos o desconto global e mais o frete.</p>
 */
public final class QuotationMapper {

    private QuotationMapper() {
    }

    // ---------------------------------------------------------------------
    // Item
    // ---------------------------------------------------------------------

    /**
     * Cria uma entidade {@link QuotationItem} a partir do DTO de request,
     * aplicando a margem de lucro no {@code unitPrice} e calculando o
     * {@code totalPrice} como
     * {@code (unitPrice × (1 + profitMargin/100)) × quantity − discount}
     * (com o desconto interpretado conforme {@code discountType}).
     */
    public static QuotationItem toItemEntity(QuotationItemRequest request,
                                             Long quotationId,
                                             BigDecimal profitMargin) {
        QuotationItem item = new QuotationItem();
        item.setQuotationId(quotationId);
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        // Preço base (sem margem) — persistido para que a edição da
        // proposta não reaplique a margem sobre o snapshot.
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
     * Cria uma entidade {@link QuotationItem} a partir do DTO permissivo
     * de simulação. Campos nulos (quantidade/preço/desconto) são tratados
     * como zero pelo cálculo — o preview pode ser disparado com o
     * formulário em estado intermediário.
     */
    public static QuotationItem toItemEntity(QuotationSimulateItemRequest request,
                                             Long quotationId,
                                             BigDecimal profitMargin) {
        QuotationItem item = new QuotationItem();
        item.setQuotationId(quotationId);
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        // Preço base (sem margem) — persistido junto do snapshot.
        item.setBaseUnitPrice(request.unitPrice());
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

    public static QuotationItemResponse toItemResponse(QuotationItem item) {
        return new QuotationItemResponse(
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

    private static BigDecimal lineSubtotal(QuotationItem item) {
        if (item.getUnitPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return item.getUnitPrice().multiply(item.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
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

    /**
     * Calcula o total líquido de uma linha, aplicando a margem de lucro
     * <b>antes</b> do desconto da própria linha:
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
        applyHeader(q, request.customerId(), request.companyId(), request.attention(),
                request.sellerId(), request.discountType(), request.discount(),
                request.validityDays(), request.paymentCondition(), request.notes(),
                request.freightType(), request.freightValue(),
                request.profitMargin(), request.carrierId());
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
        applyHeader(q, request.customerId(), request.companyId(), request.attention(),
                request.sellerId(), request.discountType(), request.discount(),
                request.validityDays(), request.paymentCondition(), request.notes(),
                request.freightType(), request.freightValue(),
                request.profitMargin(), null);
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
        if (request.customerId() != null) {
            quotation.setCustomerId(request.customerId());
        }
        if (request.companyId() != null) {
            quotation.setCompanyId(request.companyId());
        }
        if (request.attention() != null) {
            quotation.setAttention(request.attention());
        }
        if (request.sellerId() != null) {
            quotation.setSellerId(request.sellerId());
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
        if (request.freightType() != null) {
            quotation.setFreightType(request.freightType());
        }
        if (request.freightValue() != null) {
            quotation.setFreightValue(request.freightValue());
        }
        if (request.profitMargin() != null) {
            quotation.setProfitMargin(request.profitMargin());
        }
        // carrierId admite null (remoção da transportadora vinculada).
        quotation.setCarrierId(request.carrierId());
    }

    private static void applyHeader(Quotation q, Long customerId, Long companyId,
                                    String attention, Long sellerId,
                                    DiscountType discountType, BigDecimal discount,
                                    Integer validityDays, br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition paymentCondition,
                                    String notes,
                                    FreightType freightType, BigDecimal freightValue,
                                    BigDecimal profitMargin, Long carrierId) {
        q.setCustomerId(customerId);
        q.setCompanyId(companyId);
        q.setAttention(attention);
        q.setSellerId(sellerId);
        q.setDiscountType(discountType);
        q.setDiscount(discount);
        q.setValidityDays(validityDays);
        q.setPaymentCondition(paymentCondition);
        q.setNotes(notes);
        q.setFreightType(freightType);
        q.setFreightValue(freightValue);
        q.setProfitMargin(profitMargin);
        q.setCarrierId(carrierId);
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
    public static QuotationResponse toResponse(Quotation quotation, List<QuotationItem> items,
                                               String sellerName, String clientName, String clientCode,
                                               String carrierName) {
        QuotationResponse.ClientType clientType =
                (quotation.getCustomerId() != null)
                        ? QuotationResponse.ClientType.CUSTOMER
                        : QuotationResponse.ClientType.COMPANY;

        return new QuotationResponse(
                quotation.getId(),
                quotation.getNumber(),
                quotation.getIssueDate(),
                quotation.getCustomerId(),
                quotation.getCompanyId(),
                clientType,
                clientName,
                clientCode,
                quotation.getAttention(),
                quotation.getSellerId(),
                sellerName,
                items.stream().map(QuotationMapper::toItemResponse).toList(),
                quotation.getDiscountType(),
                quotation.getDiscount(),
                quotation.getValidityDays(),
                quotation.getPaymentCondition(),
                quotation.getNotes(),
                quotation.getFreightType(),
                quotation.getFreightValue(),
                quotation.getCarrierId(),
                carrierName,
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
     * Constrói o resumo a partir da entidade. Os dados do cliente (nome e
     * código) e o nome do vendedor são resolvidos pelo service e injetados
     * neste ponto.
     */
    public static QuotationSummaryResponse toSummary(Quotation quotation, String clientName,
                                                     String clientCode, String sellerName) {
        QuotationResponse.ClientType clientType =
                (quotation.getCustomerId() != null)
                        ? QuotationResponse.ClientType.CUSTOMER
                        : QuotationResponse.ClientType.COMPANY;
        Long clientId = (quotation.getCustomerId() != null)
                ? quotation.getCustomerId()
                : quotation.getCompanyId();

        return new QuotationSummaryResponse(
                quotation.getId(),
                quotation.getNumber(),
                quotation.getIssueDate(),
                clientType,
                clientId,
                clientName,
                clientCode,
                quotation.getSellerId(),
                sellerName,
                quotation.getStatus(),
                quotation.getTotalQuantity(),
                quotation.getTotal(),
                quotation.getPaymentCondition());
    }
}