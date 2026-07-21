package br.com.toppower.erp_toppower.payable.exception;

/**
 * Lançada quando a referência ao fornecedor (supplier) é inválida:
 * ausente (nula) ou inexistente no banco.
 */
public class InvalidPayableSupplierException extends RuntimeException {

    private InvalidPayableSupplierException(String message) {
        super(message);
    }

    public static InvalidPayableSupplierException nullSupplier() {
        return new InvalidPayableSupplierException(
                "A conta a pagar deve referenciar um fornecedor (supplier) cadastrado.");
    }

    public static InvalidPayableSupplierException notFound(Long supplierId) {
        return new InvalidPayableSupplierException(
                "Fornecedor não encontrado: " + supplierId);
    }
}