package br.com.toppower.erp_toppower.supplier.exception;

import java.util.UUID;

public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(UUID uuid) {
        super("Fornecedor não encontrado: " + uuid);
    }
}
