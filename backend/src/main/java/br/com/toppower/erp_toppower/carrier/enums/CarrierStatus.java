package br.com.toppower.erp_toppower.carrier.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado da transportadora no cadastro: indica se está ativa para uso
 * no sistema (seleção em propostas, notas, etc.).
 */
@Schema(name = "CarrierStatus", description = "Status da transportadora no cadastro.",
        allowableValues = {"ATIVO", "INATIVO"})
public enum CarrierStatus {
    ATIVO,
    INATIVO
}
