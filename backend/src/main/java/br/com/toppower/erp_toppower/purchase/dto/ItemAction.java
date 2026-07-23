package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ação escolhida pelo usuário para um item da NF-e na confirmação
 * da importação.
 */
@Schema(name = "ItemAction",
        description = "Ação do usuário para um item da NF-e na confirmação.",
        allowableValues = {"CADASTRAR", "ESTOQUE", "IGNORAR"})
public enum ItemAction {
    /** Cadastrar o produto (novo) e registrar entrada de estoque. */
    CADASTRAR,
    /** Apenas registrar entrada de estoque no produto existente. */
    ESTOQUE,
    /** Ignorar o item — não cadastra nem movimenta estoque. */
    IGNORAR
}