package br.com.toppower.erp_toppower.tenant.mapper;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.tenant.dto.TenantCreateRequest;
import br.com.toppower.erp_toppower.tenant.dto.TenantResponse;
import br.com.toppower.erp_toppower.tenant.dto.TenantSummary;
import br.com.toppower.erp_toppower.tenant.entity.Tenant;

public final class TenantMapper {

    private TenantMapper() {
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
     * O {@code code} NÃO é setado aqui — gerado automaticamente no service.
     */
    public static Tenant toEntity(TenantCreateRequest request) {
        Tenant tenant = new Tenant();
        tenant.setLegalName(request.legalName());
        tenant.setTradeName(request.tradeName());
        tenant.setCnpj(request.cnpj());
        tenant.setStateRegistration(request.stateRegistration());
        // stateRegistrationExempt: se não informado, assume false (default).
        tenant.setStateRegistrationExempt(
                Boolean.TRUE.equals(request.stateRegistrationExempt()));
        tenant.setMunicipalRegistration(request.municipalRegistration());
        tenant.setAddress(toEntity(request.address()));
        tenant.setStatus(request.status());
        return tenant;
    }

    public static TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getUuid(),
                tenant.getLegalName(),
                tenant.getTradeName(),
                tenant.getCode(),
                tenant.getCnpj(),
                tenant.getStateRegistration(),
                tenant.isStateRegistrationExempt(),
                tenant.getMunicipalRegistration(),
                toDto(tenant.getAddress()),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt(),
                tenant.getCreatedBy(),
                tenant.getUpdatedBy()
        );
    }

    /**
     * Resumo para dropdowns/seleção. Usa {@code tradeName} como displayName,
     * caindo para {@code legalName} quando o nome fantasia é nulo.
     */
    public static TenantSummary toSummary(Tenant tenant) {
        String displayName = (tenant.getTradeName() != null && !tenant.getTradeName().isBlank())
                ? tenant.getTradeName()
                : tenant.getLegalName();
        return new TenantSummary(
                tenant.getUuid(),
                displayName,
                tenant.getCode(),
                tenant.getCnpj()
        );
    }
}