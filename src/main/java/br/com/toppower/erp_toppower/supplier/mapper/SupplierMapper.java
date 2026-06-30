package br.com.toppower.erp_toppower.supplier.mapper;

import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.supplier.dto.AddressDto;
import br.com.toppower.erp_toppower.supplier.dto.SupplierCreateRequest;
import br.com.toppower.erp_toppower.supplier.dto.SupplierResponse;
import br.com.toppower.erp_toppower.supplier.dto.SupplierUpdateRequest;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;

public final class SupplierMapper {

    private SupplierMapper() {
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
     * O {@code status} pode ser {@code null}; o {@code @PrePersist} da entidade
     * cuida de aplicar o default {@code ATIVO}.
     */
    public static Supplier toEntity(SupplierCreateRequest request) {
        Supplier supplier = new Supplier();
        supplier.setLegalName(request.legalName());
        supplier.setTradeName(request.tradeName());
        supplier.setTaxId(request.taxId());
        supplier.setStateRegistration(request.stateRegistration());
        supplier.setMunicipalRegistration(request.municipalRegistration());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setContactName(request.contactName());
        supplier.setAddress(toEntity(request.address()));
        supplier.setStatus(request.status());
        return supplier;
    }

    public static SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getUuid(),
                supplier.getLegalName(),
                supplier.getTradeName(),
                supplier.getTaxId(),
                supplier.getStateRegistration(),
                supplier.getMunicipalRegistration(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getContactName(),
                toDto(supplier.getAddress()),
                supplier.getStatus(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt(),
                supplier.getCreatedBy(),
                supplier.getUpdatedBy()
        );
    }

    /**
     * Aplica atualização parcial (PATCH). O CNPJ (taxId) é imutável.
     */
    public static void applyUpdate(Supplier supplier, SupplierUpdateRequest request) {
        if (request.legalName() != null) {
            supplier.setLegalName(request.legalName());
        }
        if (request.tradeName() != null) {
            supplier.setTradeName(request.tradeName());
        }
        if (request.stateRegistration() != null) {
            supplier.setStateRegistration(request.stateRegistration());
        }
        if (request.municipalRegistration() != null) {
            supplier.setMunicipalRegistration(request.municipalRegistration());
        }
        if (request.email() != null) {
            supplier.setEmail(request.email());
        }
        if (request.phone() != null) {
            supplier.setPhone(request.phone());
        }
        if (request.contactName() != null) {
            supplier.setContactName(request.contactName());
        }
        if (request.address() != null) {
            supplier.setAddress(toEntity(request.address()));
        }
        if (request.status() != null) {
            supplier.setStatus(request.status());
        }
    }
}
