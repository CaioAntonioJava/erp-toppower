package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status de um item da NF-e durante o preview de importação.
 */
@Schema(name = "ItemStatus",
        description = "Status do produto na importação da NF-e.",
        allowableValues = {"NOVO", "EXISTENTE", "DIVERGENTE"})
public enum ItemStatus {
    /** Produto não cadastrado — será criado na confirmação. */
    NOVO,
    /** Produto já cadastrado e compatível — apenas entrada de estoque. */
    EXISTENTE,
    /** Produto já cadastrado mas com descrição divergente — requer atenção. */
    DIVERGENTE
}