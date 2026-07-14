package br.com.toppower.erp_toppower.organization.mapper;

import br.com.toppower.erp_toppower.organization.dto.OrganizationCreateRequest;
import br.com.toppower.erp_toppower.organization.dto.OrganizationResponse;
import br.com.toppower.erp_toppower.organization.dto.OrganizationSummary;
import br.com.toppower.erp_toppower.organization.dto.OrganizationUpdateRequest;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.user.enums.Role;

public final class OrganizationMapper {

    private OrganizationMapper() {
    }

    public static Organization toEntity(OrganizationCreateRequest request) {
        Organization org = new Organization();
        org.setCorporateName(request.corporateName());
        org.setTradeName(request.tradeName());
        org.setCnpj(request.cnpj());
        org.setStateRegistration(request.stateRegistration());
        org.setMunicipalRegistration(request.municipalRegistration());
        org.setPhone(request.phone());
        org.setEmail(request.email());
        org.setZipCode(request.zipCode());
        org.setStreet(request.street());
        org.setNumber(request.number());
        org.setDistrict(request.district());
        org.setCity(request.city());
        org.setState(request.state());
        org.setComplement(request.complement());
        org.setLogoUrl(request.logoUrl());
        org.setStatus(request.status());
        org.setProposalPrefix(request.proposalPrefix());
        org.setContractPrefix(request.contractPrefix());
        org.setContractDefaultDescription(request.contractDefaultDescription());
        return org;
    }

    public static void applyUpdate(Organization org, OrganizationUpdateRequest request) {
        if (request.corporateName() != null) org.setCorporateName(request.corporateName());
        if (request.tradeName() != null) org.setTradeName(request.tradeName());
        if (request.stateRegistration() != null) org.setStateRegistration(request.stateRegistration());
        if (request.municipalRegistration() != null) org.setMunicipalRegistration(request.municipalRegistration());
        if (request.phone() != null) org.setPhone(request.phone());
        if (request.email() != null) org.setEmail(request.email());
        if (request.zipCode() != null) org.setZipCode(request.zipCode());
        if (request.street() != null) org.setStreet(request.street());
        if (request.number() != null) org.setNumber(request.number());
        if (request.district() != null) org.setDistrict(request.district());
        if (request.city() != null) org.setCity(request.city());
        if (request.state() != null) org.setState(request.state());
        if (request.complement() != null) org.setComplement(request.complement());
        if (request.logoUrl() != null) org.setLogoUrl(request.logoUrl());
        if (request.status() != null) org.setStatus(request.status());
        if (request.proposalPrefix() != null) org.setProposalPrefix(request.proposalPrefix());
        if (request.contractPrefix() != null) org.setContractPrefix(request.contractPrefix());
        if (request.contractDefaultDescription() != null) org.setContractDefaultDescription(request.contractDefaultDescription());
    }

    public static OrganizationResponse toResponse(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getCorporateName(),
                org.getTradeName(),
                org.getCnpj(),
                org.getStateRegistration(),
                org.getMunicipalRegistration(),
                org.getPhone(),
                org.getEmail(),
                org.getZipCode(),
                org.getStreet(),
                org.getNumber(),
                org.getDistrict(),
                org.getCity(),
                org.getState(),
                org.getComplement(),
                org.getLogoUrl(),
                org.getStatus(),
                org.getProposalPrefix(),
                org.getContractPrefix(),
                org.getContractDefaultDescription(),
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }

    /**
     * Summary a partir da entidade, sem info de vínculo (role/isDefault).
     * Útil para ADMIN, que acessa todas as orgs sem {@code UserOrganization}.
     */
    public static OrganizationSummary toSummary(Organization org) {
        return new OrganizationSummary(
                org.getId(),
                org.getCorporateName(),
                org.getTradeName(),
                org.getCnpj(),
                org.getLogoUrl(),
                org.getStatus(),
                org.getProposalPrefix(),
                org.getContractPrefix(),
                org.getContractDefaultDescription(),
                null,
                false
        );
    }

    public static OrganizationSummary toSummary(Organization org, Role role, boolean isDefault) {
        return new OrganizationSummary(
                org.getId(),
                org.getCorporateName(),
                org.getTradeName(),
                org.getCnpj(),
                org.getLogoUrl(),
                org.getStatus(),
                org.getProposalPrefix(),
                org.getContractPrefix(),
                org.getContractDefaultDescription(),
                role,
                isDefault
        );
    }
}