package br.com.toppower.erp_toppower.contract.exception;

/**
 * Exceção genérica de regra de negócio do módulo de contratos.
 *
 * <p>Lançada em situações que violam invariantes do domínio, como
 * tentativa de edição de contrato {@code CONCLUIDA}, transição de
 * status inválida, ausência de dados obrigatórios ou problemas de
 * configuração da {@code Organization}. Mapeada para HTTP 409
 * (Conflito) ou 400 (Requisição Inválida) pelo handler global,
 * dependendo do contexto da chamada.</p>
 */
public class ContractBusinessException extends RuntimeException {

    public ContractBusinessException(String message) {
        super(message);
    }
}