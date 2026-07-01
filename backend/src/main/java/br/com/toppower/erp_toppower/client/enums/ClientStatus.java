package br.com.toppower.erp_toppower.client.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado do cliente no cadastro: indica se está ativo
 * (pode receber pedidos/notas) ou inativo.
 */
@Schema(name = "ClientStatus", description = "Status do cliente no cadastro.",
        allowableValues = {"ATIVO", "INATIVO"})
public enum ClientStatus {
    ATIVO,
    INATIVO
}
