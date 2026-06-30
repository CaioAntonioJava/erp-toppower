package br.com.toppower.erp_toppower.client.mapper;

import br.com.toppower.erp_toppower.client.dto.AddressDto;
import br.com.toppower.erp_toppower.client.dto.ClientCreateRequest;
import br.com.toppower.erp_toppower.client.dto.ClientResponse;
import br.com.toppower.erp_toppower.client.dto.ClientUpdateRequest;
import br.com.toppower.erp_toppower.client.entity.Client;
import br.com.toppower.erp_toppower.common.embeddable.Address;

public final class ClientMapper {

    private ClientMapper() {
    }

    public static Address toEntity(AddressDto dto) {
        Address address = new Address();
        address.setStreet(dto.street());
        address.setNumber(dto.number());
        address.setComplement(dto.complement());
        address.setNeighborhood(dto.neighborhood());
        address.setCity(dto.city());
        address.setState(dto.state());
        address.setZipCode(dto.zipCode());
        return address;
    }

    public static AddressDto toDto(Address address) {
        return new AddressDto(
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getZipCode()
        );
    }

    /**
     * Cria uma nova entidade a partir do request de criação.
     * O {@code status} pode ser {@code null}; o {@code @PrePersist} da entidade
     * cuida de aplicar o default {@code ATIVO}.
     */
    public static Client toEntity(ClientCreateRequest request) {
        Client client = new Client();
        client.setLegalName(request.legalName());
        client.setTradeName(request.tradeName());
        client.setCode(request.code());
        client.setPersonType(request.personType());
        client.setTaxId(request.taxId());
        client.setStateRegistration(request.stateRegistration());
        client.setMunicipalRegistration(request.municipalRegistration());
        client.setAddress(toEntity(request.address()));
        client.setStatus(request.status());
        return client;
    }

    public static ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getUuid(),
                client.getLegalName(),
                client.getTradeName(),
                client.getCode(),
                client.getPersonType(),
                client.getTaxId(),
                client.getStateRegistration(),
                client.getMunicipalRegistration(),
                toDto(client.getAddress()),
                client.getStatus(),
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getCreatedBy(),
                client.getUpdatedBy()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     * O taxId e personType NÃO são alteráveis (identidade fiscal).
     */
    public static void applyUpdate(Client client, ClientUpdateRequest request) {
        if (request.legalName() != null) {
            client.setLegalName(request.legalName());
        }
        if (request.tradeName() != null) {
            client.setTradeName(request.tradeName());
        }
        if (request.stateRegistration() != null) {
            client.setStateRegistration(request.stateRegistration());
        }
        if (request.municipalRegistration() != null) {
            client.setMunicipalRegistration(request.municipalRegistration());
        }
        if (request.address() != null) {
            client.setAddress(toEntity(request.address()));
        }
        if (request.status() != null) {
            client.setStatus(request.status());
        }
    }
}
