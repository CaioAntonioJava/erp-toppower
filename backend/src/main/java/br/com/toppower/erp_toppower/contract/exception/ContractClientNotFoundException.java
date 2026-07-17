package br.com.toppower.erp_toppower.contract.exception;

/**
 * Lançada quando o cliente (Customer ou Company) referenciado no contrato
 * não é encontrado no banco de dados.
 */
public class ContractClientNotFoundException extends RuntimeException {

    public ContractClientNotFoundException(String type, Long id) {
        super("Cliente do tipo " + type + " não encontrado: " + id);
    }
}