package br.com.toppower.erp_toppower.contract.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Situação de um contrato no sistema.
 *
 * <ul>
 *   <li>{@link #ATIVO} — contrato vigente, pode ser utilizado em operações
 *       de negócio. Estado inicial na criação.</li>
 *   <li>{@link #CONCLUIDO} — execução do contrato finalizada. A data de
 *       entrega ({@code delivery_date}) é preenchida automaticamente na
 *       transição para este estado. Contratos CONCLUIDOS não podem ser
 *       editados — use o endpoint {@code /reopen} para voltar a ATIVO
 *       antes de alterar.</li>
 *   <li>{@link #INATIVO} — contrato desativado (soft delete). Não é
 *       removido fisicamente, apenas marcado como inativo.</li>
 * </ul>
 */
@Schema(name = "ContractStatus",
        description = "Situação atual do contrato.",
        allowableValues = {"ATIVO", "CONCLUIDO", "INATIVO"})
public enum ContractStatus {
    ATIVO,
    CONCLUIDO,
    INATIVO
}