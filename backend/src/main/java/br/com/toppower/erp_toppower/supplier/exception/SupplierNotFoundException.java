package br.com.toppower.erp_toppower.supplier.exception;

public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(Long id) {
        super("Fornecedor não encontrado: " + id);
    }
}
