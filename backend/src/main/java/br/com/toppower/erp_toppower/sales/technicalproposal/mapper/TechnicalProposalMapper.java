package br.com.toppower.erp_toppower.sales.technicalproposal.mapper;

import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.sales.quotation.enums.DiscountType;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalAddressRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalAddressResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalConditionRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalConditionResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalCreateRequest;
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
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalCondition;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalProductItem;
import br.com.toppower.erp_toppower.sales.technicalproposal.entity.TechnicalProposalServiceItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Conversões entre entidades do agregado {@code TechnicalProposal} e
 * seus DTOs.
 *
 * <p>O {@code unitPrice} dos itens de produto e o {@code price} dos
 * itens de serviço são persistidos como snapshots do valor informado
 * pelo usuário; o {@code totalPrice} do item de produto é calculado
 * como {@code unitPrice × quantity}. Com isso, o total da proposta
 * técnica é simplesmente a soma dos itens, menos o desconto global e
 * mais o frete.</p>
 *
 * <p>Inclui:</p>
 * <ul>
 *   <li>cálculo do {@code totalPrice} de cada item de produto;</li>
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
     * DTO de request. O preço do serviço é persistido como informado
     * pelo usuário.
     */
    public static TechnicalProposalServiceItem toServiceItemEntity(
            TechnicalProposalServiceItemRequest request, Long technicalProposalId) {
        TechnicalProposalServiceItem item = new TechnicalProposalServiceItem();
        item.setTechnicalProposalId(technicalProposalId);
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setCategory(request.category());
        item.setServiceTemplateId(request.serviceTemplateId());
        return item;
    }

    public static TechnicalProposalServiceItemResponse toServiceItemResponse(
            TechnicalProposalServiceItem item) {
        return new TechnicalProposalServiceItemResponse(
                item.getId(),
                item.getDescription(),
                item.getPrice(),
                item.getCategory(),
                item.getServiceTemplateId());
    }

    // ---------------------------------------------------------------------
    // Itens de produto
    // ---------------------------------------------------------------------

    /**
     * Cria uma entidade {@link TechnicalProposalProductItem} a partir do
     * DTO de request, calculando o {@code totalPrice} como
     * {@code unitPrice × quantity}.
     */
    public static TechnicalProposalProductItem toProductItemEntity(
            TechnicalProposalProductItemRequest request, Long technicalProposalId) {
        TechnicalProposalProductItem item = new TechnicalProposalProductItem();
        item.setTechnicalProposalId(technicalProposalId);
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setTotalPrice(calculateProductItemTotalPrice(
                request.unitPrice(),
                request.quantity()));
        return item;
    }

    public static TechnicalProposalProductItemResponse toProductItemResponse(
            TechnicalProposalProductItem item) {
        return new TechnicalProposalProductItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                productLineSubtotal(item),
                item.getTotalPrice());
    }

    // ---------------------------------------------------------------------
    // Condições
    // ---------------------------------------------------------------------

    /**
     * Cria uma entidade {@link TechnicalProposalCondition} a partir do
     * DTO de request. A ordem de exibição é definida pela posição do
     * item na lista.
     */
    public static TechnicalProposalCondition toConditionEntity(
            TechnicalProposalConditionRequest request, Long technicalProposalId, int sortOrder) {
        TechnicalProposalCondition condition = new TechnicalProposalCondition();
        condition.setTechnicalProposalId(technicalProposalId);
        condition.setTitle(request.title());
        condition.setContent(request.content());
        condition.setSortOrder(sortOrder);
        return condition;
    }

    public static TechnicalProposalConditionResponse toConditionResponse(
            TechnicalProposalCondition condition) {
        return new TechnicalProposalConditionResponse(
                condition.getId(),
                condition.getTitle(),
                condition.getContent(),
                condition.getSortOrder());
    }

    private static BigDecimal productLineSubtotal(TechnicalProposalProductItem item) {
        if (item.getUnitPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return item.getUnitPrice().multiply(item.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o total de uma linha de produto:
     * {@code totalPrice = unitPrice × quantity}.
     */
    static BigDecimal calculateProductItemTotalPrice(BigDecimal unitPrice,
                                                     BigDecimal quantity) {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal gross = unitPrice.multiply(quantity);
        return gross.setScale(2, RoundingMode.HALF_UP);
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
        applyHeader(tp, request.customerId(), request.companyId(),
                toAddress(request.address()),
                request.description(),
                request.revision(),
                request.technicalResponsible(), request.email(), request.phone(),
                request.discountType(), request.discount(),
                request.freightValue(), request.deliveryDeadline(),
                request.paymentCondition(), request.validity(),
                request.deliveryType(), request.notes(), request.carrierId(),
                request.generalPrice());
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
        tp.setCustomerId(null);
        tp.setCompanyId(null);
        tp.setDescription(null);
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
        if (request.customerId() != null) {
            tp.setCustomerId(request.customerId());
        }
        if (request.companyId() != null) {
            tp.setCompanyId(request.companyId());
        }
        if (request.address() != null) {
            tp.setAddress(toAddress(request.address()));
        }
        if (request.description() != null) {
            tp.setDescription(request.description());
        }
        if (request.revision() != null) {
            tp.setRevision(request.revision());
        }
        if (request.technicalResponsible() != null) {
            tp.setTechnicalResponsible(emptyToNull(request.technicalResponsible()));
        }
        if (request.email() != null) {
            tp.setEmail(emptyToNull(request.email()));
        }
        if (request.phone() != null) {
            tp.setPhone(emptyToNull(request.phone()));
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
        // carrierId admite null (remoção da transportadora vinculada).
        tp.setCarrierId(request.carrierId());
        // generalPrice admite null (remoção do preço geral).
        tp.setGeneralPrice(request.generalPrice());
    }

    private static void applyHeader(TechnicalProposal tp, Long customerId, Long companyId,
                                    Address address, String description,
                                    Integer revision,
                                    String technicalResponsible, String email, String phone,
                                    DiscountType discountType,
                                    BigDecimal discount, BigDecimal freightValue,
                                    String deliveryDeadline,
                                    br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition paymentCondition,
                                    String validity,
                                    br.com.toppower.erp_toppower.sales.quotation.enums.FreightType deliveryType,
                                    String notes, Long carrierId,
                                    BigDecimal generalPrice) {
        tp.setCustomerId(customerId);
        tp.setCompanyId(companyId);
        tp.setAddress(address);
        tp.setDescription(description);
        tp.setRevision(revision);
        tp.setTechnicalResponsible(technicalResponsible);
        tp.setEmail(email);
        tp.setPhone(phone);
        tp.setDiscountType(discountType);
        tp.setDiscount(discount);
        tp.setFreightValue(freightValue);
        tp.setDeliveryDeadline(deliveryDeadline);
        tp.setPaymentCondition(paymentCondition);
        tp.setValidity(validity);
        tp.setDeliveryType(deliveryType);
        tp.setNotes(notes);
        tp.setCarrierId(carrierId);
        tp.setGeneralPrice(generalPrice);
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
            List<TechnicalProposalServiceItem> serviceItems,
            List<TechnicalProposalProductItem> productItems,
            List<TechnicalProposalCondition> conditions,
            String clientName, String clientCode,
            String carrierName) {

        TechnicalProposalResponse.ClientType clientType =
                (tp.getCustomerId() != null)
                        ? TechnicalProposalResponse.ClientType.CUSTOMER
                        : TechnicalProposalResponse.ClientType.COMPANY;

        return new TechnicalProposalResponse(
                tp.getId(),
                tp.getPrefix(),
                tp.getSequence(),
                tp.getYear(),
                tp.formattedCode(),
                tp.getCustomerId(),
                tp.getCompanyId(),
                clientType,
                clientName,
                clientCode,
                toAddressResponse(tp.getAddress()),
                tp.getDescription(),
                tp.getRevision(),
                tp.getTechnicalResponsible(),
                tp.getEmail(),
                tp.getPhone(),
                tp.getStatus(),
                tp.getStartDate(),
                tp.getDeliveryDate(),
                serviceItems.stream().map(TechnicalProposalMapper::toServiceItemResponse).toList(),
                productItems.stream().map(TechnicalProposalMapper::toProductItemResponse).toList(),
                conditions.stream().map(TechnicalProposalMapper::toConditionResponse).toList(),
                tp.getDiscountType(),
                tp.getDiscount(),
                tp.getFreightValue(),
                tp.getDeliveryDeadline(),
                tp.getPaymentCondition(),
                tp.getValidity(),
                tp.getDeliveryType(),
                tp.getNotes(),
                tp.getCarrierId(),
                carrierName,
                tp.getGeneralPrice(),
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
            String clientName, String clientCode) {
        TechnicalProposalResponse.ClientType clientType =
                (tp.getCustomerId() != null)
                        ? TechnicalProposalResponse.ClientType.CUSTOMER
                        : TechnicalProposalResponse.ClientType.COMPANY;
        Long clientId = (tp.getCustomerId() != null)
                ? tp.getCustomerId()
                : tp.getCompanyId();

        return new TechnicalProposalSummaryResponse(
                tp.getId(),
                tp.formattedCode(),
                clientType,
                clientId,
                clientName,
                clientCode,
                tp.getStatus(),
                tp.getStartDate(),
                tp.getDeliveryDate(),
                tp.getTotal(),
                tp.getGeneralPrice(),
                tp.getPaymentCondition());
    }
}