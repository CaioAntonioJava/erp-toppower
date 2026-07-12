package br.com.toppower.erp_toppower.contract.mapper;

import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.contract.dto.ContractAddressRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractAddressResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractClauseRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractClauseResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractCreateRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractProductItemRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractProductItemResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractServiceItemRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractServiceItemResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractSummaryResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractUpdateRequest;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.contract.entity.ContractClause;
import br.com.toppower.erp_toppower.contract.entity.ContractProductItem;
import br.com.toppower.erp_toppower.contract.entity.ContractServiceItem;

import java.util.List;
import java.util.UUID;

/**
 * Conversões entre a entidade {@link Contract} e seus DTOs.
 *
 * <p>Esta classe é um utilitário {@code final} com métodos
 * {@code static} (mesmo padrão de {@code TechnicalProposalMapper},
 * {@code QuotationMapper}, etc.) — nenhum mapeamento usa MapStruct.</p>
 *
 * <p>O código comercial ({@code prefix}, {@code sequence}, {@code year})
 * e o status <b>não</b> são definidos por este mapper — são gerados pelo
 * service e pelo {@code @PrePersist} da entidade, respectivamente.</p>
 */
public final class ContractMapper {

    private ContractMapper() {
    }

    // ---------------------------------------------------------------------
    // Endereço
    // ---------------------------------------------------------------------

    /**
     * Converte o DTO permissivo de endereço no {@link Address} embutido.
     * Retorna {@code null} quando o request é nulo — endereço opcional.
     */
    public static Address toAddress(ContractAddressRequest request) {
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

    public static ContractAddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new ContractAddressResponse(
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getZipCode());
    }

    // ---------------------------------------------------------------------
    // Cláusulas
    // ---------------------------------------------------------------------

    public static ContractClause toClauseEntity(
            ContractClauseRequest request, UUID contractUuid) {
        ContractClause clause = new ContractClause();
        clause.setContractUuid(contractUuid);
        clause.setDescription(request.description());
        return clause;
    }

    public static ContractClauseResponse toClauseResponse(
            ContractClause clause) {
        return new ContractClauseResponse(
                clause.getUuid(), clause.getDescription());
    }

    // ---------------------------------------------------------------------
    // Itens de serviço
    // ---------------------------------------------------------------------

    public static ContractServiceItem toServiceItemEntity(
            ContractServiceItemRequest request, UUID contractUuid) {
        ContractServiceItem item = new ContractServiceItem();
        item.setContractUuid(contractUuid);
        item.setDescription(request.description());
        return item;
    }

    public static ContractServiceItemResponse toServiceItemResponse(
            ContractServiceItem item) {
        return new ContractServiceItemResponse(
                item.getUuid(), item.getDescription());
    }

    // ---------------------------------------------------------------------
    // Itens de produto
    // ---------------------------------------------------------------------

    public static ContractProductItem toProductItemEntity(
            ContractProductItemRequest request, UUID contractUuid) {
        ContractProductItem item = new ContractProductItem();
        item.setContractUuid(contractUuid);
        item.setProductUuid(request.productUuid());
        item.setQuantity(request.quantity());
        return item;
    }

    public static ContractProductItemResponse toProductItemResponse(
            ContractProductItem item) {
        return new ContractProductItemResponse(
                item.getUuid(), item.getProductUuid(), item.getQuantity());
    }

    // ---------------------------------------------------------------------
    // Header — create / update
    // ---------------------------------------------------------------------

    /**
     * Cria a entidade {@link Contract} (header) a partir do request de
     * criação. O código (prefix/sequence/year) e o status NÃO são
     * definidos aqui — código é gerado pelo service e status recebe o
     * default {@code ABERTA} no {@code @PrePersist}. A invariante
     * "exatamente um entre customerUuid e companyUuid" é validada no
     * service.
     */
    public static Contract toEntity(ContractCreateRequest request) {
        Contract c = new Contract();
        c.setCustomerUuid(request.customerUuid());
        c.setCompanyUuid(request.companyUuid());
        c.setAddress(toAddress(request.address()));
        c.setDescription(request.description());
        c.setServicesDescription(request.servicesDescription());
        c.setProductsDescription(request.productsDescription());
        c.setStartDate(request.startDate());
        c.setTotalValue(request.totalValue());
        return c;
    }

    /**
     * Aplica uma atualização parcial (PATCH) sobre a entidade carregada.
     * Apenas os campos não nulos do request sobrescrevem o estado atual.
     *
     * <p>Strings vazias ({@code ""}) são convertidas para {@code null}
     * para permitir "limpar" o valor dos campos opcionais.</p>
     */
    public static void applyUpdate(Contract c, ContractUpdateRequest request) {
        if (request.customerUuid() != null) {
            c.setCustomerUuid(request.customerUuid());
        }
        if (request.companyUuid() != null) {
            c.setCompanyUuid(request.companyUuid());
        }
        if (request.address() != null) {
            c.setAddress(toAddress(request.address()));
        }
        if (request.description() != null) {
            c.setDescription(request.description());
        }
        if (request.servicesDescription() != null) {
            c.setServicesDescription(emptyToNull(request.servicesDescription()));
        }
        if (request.productsDescription() != null) {
            c.setProductsDescription(emptyToNull(request.productsDescription()));
        }
        if (request.startDate() != null) {
            c.setStartDate(request.startDate());
        }
        if (request.totalValue() != null) {
            c.setTotalValue(request.totalValue());
        }
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
     * Constrói a resposta completa a partir da entidade. O nome e o
     * código do cliente (PF ou PJ) são resolvidos pelo service e
     * injetados aqui, evitando um round-trip adicional no frontend.
     */
    public static ContractResponse toResponse(Contract c,
                                              ContractResponse.ClientType clientType,
                                              String clientName,
                                              String clientCode,
                                              List<ContractClause> clauses,
                                              List<ContractServiceItem> serviceItems,
                                              List<ContractProductItem> productItems) {
        return new ContractResponse(
                c.getUuid(),
                c.getPrefix(),
                c.getSequence(),
                c.getYear(),
                c.formattedCode(),
                c.getCustomerUuid(),
                c.getCompanyUuid(),
                clientType,
                clientName,
                clientCode,
                toAddressResponse(c.getAddress()),
                c.getDescription(),
                clauses.stream().map(ContractMapper::toClauseResponse).toList(),
                c.getServicesDescription(),
                c.getProductsDescription(),
                c.getStatus(),
                c.getStartDate(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getCreatedBy(),
                c.getUpdatedBy(),
                serviceItems.stream().map(ContractMapper::toServiceItemResponse).toList(),
                productItems.stream().map(ContractMapper::toProductItemResponse).toList(),
                c.getTotalValue());
    }

    /**
     * Constrói o resumo de listagem a partir da entidade. Inclui um
     * preview curto da descrição (até 120 caracteres) para exibição
     * rápida na tabela.
     */
    public static ContractSummaryResponse toSummary(Contract c,
                                                    ContractResponse.ClientType clientType,
                                                    String clientName,
                                                    String clientCode) {
        String preview = (c.getDescription() == null) ? null : abbreviate(c.getDescription(), 120);
        UUID clientUuid = clientType == ContractResponse.ClientType.CUSTOMER
                ? c.getCustomerUuid()
                : c.getCompanyUuid();
        return new ContractSummaryResponse(
                c.getUuid(),
                c.formattedCode(),
                clientType,
                clientUuid,
                clientName,
                clientCode,
                c.getStatus(),
                c.getStartDate(),
                preview);
    }

    private static String abbreviate(String value, int max) {
        if (value == null) {
            return null;
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max).trim() + "…";
    }
}