package br.com.toppower.erp_toppower.contract.exception;

/**
 * Lançada quando uma transição de status de contrato não é permitida
 * a partir do estado atual (ex.: tentar concluir um contrato que não
 * está ATIVO, ou tentar reabrir um contrato que não está CONCLUIDO).
 *
 * <p>Mapeada para HTTP 409 CONFLICT pelo {@code GlobalExceptionHandler},
 * indicando que o estado atual do recurso impede a operação solicitada.</p>
 */
public class ContractStatusTransitionException extends RuntimeException {

    public ContractStatusTransitionException(String message) {
        super(message);
    }
}