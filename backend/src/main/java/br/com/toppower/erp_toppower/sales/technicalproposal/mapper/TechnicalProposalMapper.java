package br.com.toppower.erp_toppower.sales.technicalproposal.mapper;

import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalAddressRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalAddressResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalCreateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalObjectiveRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalObjectiveResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalProductItemRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalProductItemResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalServiceItemRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalServiceItemResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSimulateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSimulateResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSummaryResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalUpdateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposal;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalObjective;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalProductItem;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalServiceItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Conversões entre entidades do agregado {@code TechnicalProposal} e
 * seus DTOs.
 *
 * <p>A margem de lucro ({@code profitMargin}) do header é aplicada
 * <b>item a item</b> aqui: tanto o {@code unitPrice} dos itens de
 * produto quanto o {@code price} dos itens de serviço já persistem com
 * a margem embutida; o {@code totalPrice} do item de produto é
 * calculado sobre esse preço majorado, líquido do desconto da própria
 * linha. Com isso, o total da proposta técnica é simplesmente a soma
 * dos itens (já com margem e já líquido do desconto por item), menos o
 * desconto global e mais o frete.</p>
 *
 * <p>Inclui:</p>
 * <ul>
 *   <li>cálculo do {@code totalPrice} de cada item de produto (líquido,
 *       considerando o desconto por item e a margem de lucro);</li>
 *   <li>aplicação da margem sobre o {@code price} de cada item de
 *       serviço (serviços não têm quantidade nem desconto);</li>
 *   <li>construção do endereço embutido a partir do DTO permissivo;</li>
 *   <li>aplicação de PATCH parcial sobre o header.</li>
 * </ul>
 */
public final class TechnicalProposalMapper {

    private TechnicalProposalMapper() {
    }

    // ---------------------------------------------------------------------
    // Itens de serviço
    // ---------------------------------------------------------------------

    /**
     * Cria uma entidade {@link TechnicalProposalServiceItem} a partir do
     * DTO de request, aplicando a margem de lucro no {@code price} do
     * serviço através do fator {@code (1 + profitMargin / 100)}.
     */
    public static TechnicalProposalServiceItem toServiceItemEntity(
            TechnicalProposalServiceItemRequest request, UUID technicalProposalUuid,
            BigDecimal profitMargin) {
        TechnicalProposalServiceItem item = new TechnicalProposalServiceItem();
        item.setTechnicalProposalUuid(technicalProposalUuid);
        item.setDescription(request.description());
        // Preço base (sem margem) — persistido junto do snapshot.
        item.setBasePrice(request.price());
        // Snapshot final: basePrice × (1 + profitMargin/100).
        item.setPrice(applyProfitMargin(request.price(), profitMargin));
        return item;
    }

    public static TechnicalProposalServiceItemResponse toServiceItemResponse(
            TechnicalProposalServiceItem item) {
        return new TechnicalProposalServiceItemResponse(
                item.getUuid(),
                item.getDescription(),
                item.getPrice(),
                item.getBasePrice());
    }

    // ---------------------------------------------------------------------
    // Itens de produto
    // ---------------------------------------------------------------------

    /**
     * Cria uma entidade {@link TechnicalProposalProductItem} a partir do
     * DTO de request, aplicando a margem de lucro no {@code unitPrice} e
     * calculando o {@code totalPrice} como
     * {@code (unitPrice × (1 + profitMargin/100)) × quantity − discount}
     * (com o desconto interpretado conforme {@code discountType}).
     */
    public static TechnicalProposalProductItem toProductItemEntity(
            TechnicalProposalProductItemRequest request, UUID technicalProposalUuid,
            BigDecimal profitMargin) {
        TechnicalProposalProductItem item = new TechnicalProposalProductItem();
        item.setTechnicalProposalUuid(technicalProposalUuid);
        item.setProductUuid(request.productUuid());
        item.setQuantity(request.quantity());
        // Preço base (sem margem) — persistido para que a edição da
        // proposta não reaplique a margem sobre o snapshot.
        item.setBaseUnitPrice(request.unitPrice());
        // Snapshot final: baseUnitPrice × (1 + profitMargin/100).
        item.setUnitPrice(applyProfitMargin(request.unitPrice(), profitMargin));
        item.setDiscountType(request.discountType());
        item.setDiscount(request.discount());
        item.setTotalPrice(calculateProductItemTotalPrice(
                request.unitPrice(),
                request.quantity(),
                request.discount(),
                request.discountType(),
                profitMargin));
        return item;
    }

    public static TechnicalProposalProductItemResponse toProductItemResponse(
            TechnicalProposalProductItem item) {
        return new TechnicalProposalProductItemResponse(
                item.getUuid(),
                item.getProductUuid(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getBaseUnitPrice(),
                productLineSubtotal(item),
                item.getDiscountType(),
                item.getDiscount(),
                item.getTotalPrice());
    }

    private static BigDecimal productLineSubtotal(TechnicalProposalProductItem item) {
        if (item.getUnitPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return item.getUnitPrice().multiply(item.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Aplica a margem de lucro como multiplicação percentual sobre o
     * {@code unitPrice}/{@code price}, através do fator
     * {@code (1 + profitMargin / 100)}. Retorna {@code ZERO} quando o
     * valor for nulo e o próprio valor (arredondado para 2 casas) quando
     * a margem for nula ou zero.
     */
    static BigDecimal applyProfitMargin(BigDecimal value, BigDecimal profitMargin) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (profitMargin == null || profitMargin.signum() == 0) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(
                profitMargin.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return value.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o total líquido de uma linha de produto, aplicando a
     * margem de lucro <b>antes</b> do desconto da própria linha:
     * <pre>
     *   unitPriceComMargem = unitPrice × (1 + profitMargin/100)
     *   gross              = unitPriceComMargem × quantity
     *   desconto           = gross × discount%        (se PERCENT)
     *                      | discount (R$ fixo)       (se AMOUNT)
     *   totalPrice         = gross − desconto
     * </pre>
     * Equivalente ao usado em {@code QuotationMapper.calculateItemTotalPrice},
     * mantido como helper local para isolar este módulo do de cotação.
     */
    static BigDecimal calculateProductItemTotalPrice(BigDecimal unitPrice,
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
    // Endereço
    // ---------------------------------------------------------------------

    /**
     * Converte o DTO permissivo de endereço no {@link Address} embutido.
     * Retorna {@code null} quando o request é nulo — endereço opcional.
     * Todos os campos do {@link Address} ficam nullable aqui.
     */
    public static Address toAddress(TechnicalProposalAddressRequest request) {
        if (request == null) {
            return null;
        }
        Address address = new Address();
        address.setStreet(request.street());
        address.setNumber(request.number());
        address.setComplement(request.complement());
        address.setNeighborhood(request.neighborhood());
        address.setCity(request.city());
        address.setState(request.state());
        address.setZipCode(request.zipCode());
        return address;
    }

    public static TechnicalProposalAddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new TechnicalProposalAddressResponse(
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getZipCode());
    }

    // ---------------------------------------------------------------------
    // Objetivos
    // ---------------------------------------------------------------------

    public static TechnicalProposalObjective toObjectiveEntity(
            TechnicalProposalObjectiveRequest request, UUID technicalProposalUuid) {
        TechnicalProposalObjective objective = new TechnicalProposalObjective();
        objective.setTechnicalProposalUuid(technicalProposalUuid);
        objective.setDescription(request.description());
        return objective;
    }

    public static TechnicalProposalObjectiveResponse toObjectiveResponse(
            TechnicalProposalObjective objective) {
        return new TechnicalProposalObjectiveResponse(
                objective.getUuid(), objective.getDescription());
    }

    // ---------------------------------------------------------------------
    // Header
    // ---------------------------------------------------------------------

    /**
     * Cria a entidade {@link TechnicalProposal} (header) a partir do
     * request de criação. O código (prefix/sequence/year) e o status NÃO
     * são definidos aqui — código é gerado pelo service e status recebe
     * o default {@code ABERTA} no {@code @PrePersist}.
     */
    public static TechnicalProposal toEntity(TechnicalProposalCreateRequest request) {
        TechnicalProposal tp = new TechnicalProposal();
        applyHeader(tp, request.customerUuid(), request.companyUuid(),
                toAddress(request.address()),
                request.description(),
                request.technicalResponsible(), request.email(),
                request.startDate(), request.endDate(),
                request.profitMargin(), request.discountType(), request.discount(),
                request.freightValue(), request.deliveryDeadline(),
                request.paymentCondition(), request.validity(),
                request.deliveryType(), request.notes(), request.carrierUuid());
        return tp;
    }

    /**
     * Cria a entidade {@link TechnicalProposal} (header) a partir do
     * request permissivo de simulação. Apenas os campos relevantes para
     * o cálculo são populados; campos nulos são tolerados (o cálculo
     * trata como zero). Não define código nem status — o simulate não
     * persiste.
     */
    public static TechnicalProposal toEntity(TechnicalProposalSimulateRequest request) {
        TechnicalProposal tp = new TechnicalProposal();
        // O simulate só precisa dos campos que afetam o cálculo de totais.
        tp.setCustomerUuid(null);
        tp.setCompanyUuid(null);
        tp.setDescription(null);
        tp.setStartDate(null);
        tp.setEndDate(null);
        tp.setProfitMargin(request.profitMargin() != null ? request.profitMargin() : BigDecimal.ZERO);
        tp.setDiscountType(request.discountType());
        tp.setDiscount(request.discount());
        tp.setFreightValue(request.freightValue());
        tp.setDeliveryType(request.deliveryType());
        return tp;
    }

    /**
     * Aplica uma atualização parcial (PATCH) sobre a entidade carregada.
     * Apenas os campos não nulos do request sobrescrevem o estado atual.
     *
     * <p>As listas de itens (serviços/produtos) são tratadas como
     * substituição completa — quando informadas, os itens anteriores são
     * removidos (caller responsável) e os novos itens são inseridos.</p>
     *
     * <p>O campo {@code address} recebe tratamento especial: quando o
     * request o envia como nulo, o endereço existente é removido; quando
     * enviado vazio (todos os campos nulos), também é removido; caso
     * contrário, sobrescreve o endereço atual.</p>
     */
    public static void applyUpdate(TechnicalProposal tp, TechnicalProposalUpdateRequest request) {
        if (request.customerUuid() != null) {
            tp.setCustomerUuid(request.customerUuid());
        }
        if (request.companyUuid() != null) {
            tp.setCompanyUuid(request.companyUuid());
        }
        if (request.address() != null) {
            tp.setAddress(toAddress(request.address()));
        }
        if (request.description() != null) {
            tp.setDescription(request.description());
        }
        if (request.technicalResponsible() != null) {
            tp.setTechnicalResponsible(emptyToNull(request.technicalResponsible()));
        }
        if (request.email() != null) {
            tp.setEmail(emptyToNull(request.email()));
        }
        if (request.startDate() != null) {
            tp.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            tp.setEndDate(request.endDate());
        }
        if (request.profitMargin() != null) {
            tp.setProfitMargin(request.profitMargin());
        }
        if (request.discountType() != null) {
            tp.setDiscountType(request.discountType());
        }
        if (request.discount() != null) {
            tp.setDiscount(request.discount());
        }
        if (request.freightValue() != null) {
            tp.setFreightValue(request.freightValue());
        }
        if (request.deliveryDeadline() != null) {
            tp.setDeliveryDeadline(request.deliveryDeadline());
        }
        if (request.paymentCondition() != null) {
            tp.setPaymentCondition(request.paymentCondition());
        }
        if (request.validity() != null) {
            tp.setValidity(request.validity());
        }
        if (request.deliveryType() != null) {
            tp.setDeliveryType(request.deliveryType());
        }
        if (request.notes() != null) {
            tp.setNotes(request.notes());
        }
        // carrierUuid admite null (remoção da transportadora vinculada).
        tp.setCarrierUuid(request.carrierUuid());
    }

    private static void applyHeader(TechnicalProposal tp, UUID customerUuid, UUID companyUuid,
                                    Address address, String description,
                                    String technicalResponsible, String email,
                                    java.time.LocalDate startDate,
                                    java.time.LocalDate endDate,
                                    BigDecimal profitMargin, DiscountType discountType,
                                    BigDecimal discount, BigDecimal freightValue,
                                    String deliveryDeadline,
                                    br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition paymentCondition,
                                    String validity,
                                    br.com.toppower.erp_toppower.sales.quotation.enums.FreightType deliveryType,
                                    String notes, UUID carrierUuid) {
        tp.setCustomerUuid(customerUuid);
        tp.setCompanyUuid(companyUuid);
        tp.setAddress(address);
        tp.setDescription(description);
        tp.setTechnicalResponsible(technicalResponsible);
        tp.setEmail(email);
        tp.setStartDate(startDate);
        tp.setEndDate(endDate);
        tp.setProfitMargin(profitMargin);
        tp.setDiscountType(discountType);
        tp.setDiscount(discount);
        tp.setFreightValue(freightValue);
        tp.setDeliveryDeadline(deliveryDeadline);
        tp.setPaymentCondition(paymentCondition);
        tp.setValidity(validity);
        tp.setDeliveryType(deliveryType);
        tp.setNotes(notes);
        tp.setCarrierUuid(carrierUuid);
    }

    /**
     * Converte uma string vazia (após {@code trim()}) em {@code null},
     * preservando qualquer outro valor (incluindo espaços no meio do
     * texto). Usado pelos campos opcionais do PATCH que aceitam string
     * vazia para indicar "limpar o valor".
     */
    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ---------------------------------------------------------------------
    // Responses
    // ---------------------------------------------------------------------

    /**
     * Constrói a resposta completa a partir da entidade já com totais
     * calculados (via {@code recalculateTotals}) e das listas de itens.
     * O nome e o código do cliente são resolvidos pelo service e
     * injetados aqui, evitando um round-trip adicional no frontend.
     */
    public static TechnicalProposalResponse toResponse(
            TechnicalProposal tp,
            List<TechnicalProposalObjective> objectives,
            List<TechnicalProposalServiceItem> serviceItems,
            List<TechnicalProposalProductItem> productItems,
            String clientName, String clientCode,
            String carrierName) {

        TechnicalProposalResponse.ClientType clientType =
                (tp.getCustomerUuid() != null)
                        ? TechnicalProposalResponse.ClientType.CUSTOMER
                        : TechnicalProposalResponse.ClientType.COMPANY;

        return new TechnicalProposalResponse(
                tp.getUuid(),
                tp.getPrefix(),
                tp.getSequence(),
                tp.getYear(),
                tp.formattedCode(),
                tp.getCustomerUuid(),
                tp.getCompanyUuid(),
                clientType,
                clientName,
                clientCode,
                toAddressResponse(tp.getAddress()),
                objectives.stream().map(TechnicalProposalMapper::toObjectiveResponse).toList(),
                tp.getDescription(),
                tp.getTechnicalResponsible(),
                tp.getEmail(),
                tp.getStatus(),
                tp.getStartDate(),
                tp.getEndDate(),
                tp.getDeliveryDate(),
                serviceItems.stream().map(TechnicalProposalMapper::toServiceItemResponse).toList(),
                productItems.stream().map(TechnicalProposalMapper::toProductItemResponse).toList(),
                tp.getProfitMargin(),
                tp.getDiscountType(),
                tp.getDiscount(),
                tp.getFreightValue(),
                tp.getDeliveryDeadline(),
                tp.getPaymentCondition(),
                tp.getValidity(),
                tp.getDeliveryType(),
                tp.getNotes(),
                tp.getCarrierUuid(),
                carrierName,
                tp.getServicesSubtotal(),
                tp.getProductsSubtotal(),
                tp.getSubtotal(),
                tp.getGlobalDiscountValue(),
                tp.getTotal(),
                tp.getCreatedAt(),
                tp.getUpdatedAt(),
                tp.getCreatedBy(),
                tp.getUpdatedBy());
    }

    /**
     * Constrói o resumo de simulação (totais sem persistência).
     */
    public static TechnicalProposalSimulateResponse toSimulateResponse(
            TechnicalProposal tp,
            List<TechnicalProposalServiceItem> serviceItems,
            List<TechnicalProposalProductItem> productItems) {
        return new TechnicalProposalSimulateResponse(
                serviceItems.stream().map(TechnicalProposalMapper::toServiceItemResponse).toList(),
                productItems.stream().map(TechnicalProposalMapper::toProductItemResponse).toList(),
                tp.getServicesSubtotal(),
                tp.getProductsSubtotal(),
                tp.getSubtotal(),
                tp.getGlobalDiscountValue(),
                tp.getTotal());
    }

    /**
     * Constrói o resumo de listagem a partir da entidade já com totais
     * calculados. Os dados do cliente (nome e código) são resolvidos
     * pelo service e injetados aqui.
     */
    public static TechnicalProposalSummaryResponse toSummary(
            TechnicalProposal tp,
            List<TechnicalProposalObjective> objectives,
            String clientName, String clientCode) {
        TechnicalProposalResponse.ClientType clientType =
                (tp.getCustomerUuid() != null)
                        ? TechnicalProposalResponse.ClientType.CUSTOMER
                        : TechnicalProposalResponse.ClientType.COMPANY;
        UUID clientUuid = (tp.getCustomerUuid() != null)
                ? tp.getCustomerUuid()
                : tp.getCompanyUuid();

        return new TechnicalProposalSummaryResponse(
                tp.getUuid(),
                tp.formattedCode(),
                clientType,
                clientUuid,
                clientName,
                clientCode,
                objectives.stream().map(TechnicalProposalMapper::toObjectiveResponse).toList(),
                tp.getStatus(),
                tp.getStartDate(),
                tp.getEndDate(),
                tp.getDeliveryDate(),
                tp.getTotal(),
                tp.getPaymentCondition());
    }
}