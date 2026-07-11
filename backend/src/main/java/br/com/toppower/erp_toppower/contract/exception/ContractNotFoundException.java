package br.com.toppower.erp_toppower.contract.exception;

import java.util.UUID;

/**
 * Exceção lançada quando nenhum contrato é encontrado para o
 * identificador (UUID) ou código (ex.: {@code CT-001-2026})
 * informados. Mapeada para HTTP 404 pelo handler global.
 */
public class ContractNotFoundException extends RuntimeException {

    public ContractNotFoundException(UUID id) {
        super("Contrato não encontrado: " + id);
    }

    public ContractNotFoundException(String code) {
        super("Contrato não encontrado: " + code);
    }
}