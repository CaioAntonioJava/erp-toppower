package br.com.toppower.erp_toppower.company.mapper;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.company.dto.CompanyCreateRequest;
import br.com.toppower.erp_toppower.company.dto.CompanyResponse;
import br.com.toppower.erp_toppower.company.dto.CompanyUpdateRequest;
import br.com.toppower.erp_toppower.company.entity.Company;

public final class CompanyMapper {

    private CompanyMapper() {
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
     * <ul>
     *   <li>O {@code code} (código interno) NÃO é setado aqui — ele é gerado
     *   automaticamente no service (ex.: {@code EMP000001}) antes do
     *   {@code save}.</li>
     *   <li>O {@code status} pode ser {@code null}; o {@code @PrePersist} da
     *   entidade cuida de aplicar o default {@code ATIVO}.</li>
     * </ul>
     */
    public static Company toEntity(CompanyCreateRequest request) {
        Company company = new Company();
        company.setLegalName(request.legalName());
        company.setTradeName(request.tradeName());
        company.setCnpj(request.cnpj());
        company.setStateRegistration(request.stateRegistration());
        company.setMunicipalRegistration(request.municipalRegistration());
        company.setAddress(toEntity(request.address()));
        company.setStatus(request.status());
        return company;
    }

    public static CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getUuid(),
                company.getLegalName(),
                company.getTradeName(),
                company.getCode(),
                company.getCnpj(),
                company.getStateRegistration(),
                company.getMunicipalRegistration(),
                toDto(company.getAddress()),
                company.getStatus(),
                company.getCreatedAt(),
                company.getUpdatedAt(),
                company.getCreatedBy(),
                company.getUpdatedBy()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     * <ul>
     *   <li>O CNPJ NÃO é alterável (identidade fiscal).</li>
     *   <li>O {@code code} NÃO é alterável — é gerado uma única vez no
     *   momento do cadastro e nunca muda.</li>
     * </ul>
     */
    public static void applyUpdate(Company company, CompanyUpdateRequest request) {
        if (request.legalName() != null) {
            company.setLegalName(request.legalName());
        }
        if (request.tradeName() != null) {
            company.setTradeName(request.tradeName());
        }
        if (request.stateRegistration() != null) {
            company.setStateRegistration(request.stateRegistration());
        }
        if (request.municipalRegistration() != null) {
            company.setMunicipalRegistration(request.municipalRegistration());
        }
        if (request.address() != null) {
            company.setAddress(toEntity(request.address()));
        }
        if (request.status() != null) {
            company.setStatus(request.status());
        }
    }
}
