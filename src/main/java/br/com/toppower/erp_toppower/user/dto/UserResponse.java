package br.com.toppower.erp_toppower.user.dto;

import br.com.toppower.erp_toppower.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID uuid,
        String email,
        Role role
) {
}
