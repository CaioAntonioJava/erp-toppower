package br.com.toppower.erp_toppower.profile.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado do perfil no sistema: indica se o usuário vinculado
 * pode autenticar e operar normalmente.
 */
@Schema(name = "ProfileStatus", description = "Status do perfil no sistema.",
        allowableValues = {"ATIVO", "INATIVO"})
public enum ProfileStatus {
    ATIVO,
    INATIVO
}
