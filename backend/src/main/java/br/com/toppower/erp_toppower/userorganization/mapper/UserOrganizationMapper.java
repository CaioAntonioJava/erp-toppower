package br.com.toppower.erp_toppower.userorganization.mapper;

import br.com.toppower.erp_toppower.userorganization.dto.UserOrganizationResponse;
import br.com.toppower.erp_toppower.userorganization.entity.UserOrganization;

public final class UserOrganizationMapper {

    private UserOrganizationMapper() {
    }

    public static UserOrganizationResponse toResponse(UserOrganization uo) {
        return new UserOrganizationResponse(
                uo.getUuid(),
                uo.getUser().getUuid(),
                uo.getUser().getEmail(),
                uo.getOrganization().getUuid(),
                uo.getOrganization().getCorporateName(),
                uo.getRole(),
                uo.isDefault(),
                uo.getCreatedAt()
        );
    }
}