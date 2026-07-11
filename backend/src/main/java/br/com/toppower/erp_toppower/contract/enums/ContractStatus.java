package br.com.toppower.erp_toppower.contract.enums;

/**
 * Status do ciclo de vida de um {@code Contract}.
 *
 * <p>Mesmo modelo de estados utilizado pela Proposta Técnica, reaproveitando
 * a semântica:</p>
 * <ul>
 *   <li>{@link #ABERTA} — contrato recém-criado, ainda em negociação.
 *       Permite edições.</li>
 *   <li>{@link #EM_ANDAMENTO} — contrato assinado e em execução.
 *       Permite edições controladas.</li>
 *   <li>{@link #CONCLUIDA} — contrato encerrado. Não permite edições.</li>
 * </ul>
 */
public enum ContractStatus {

    ABERTA,
    EM_ANDAMENTO,
    CONCLUIDA
}