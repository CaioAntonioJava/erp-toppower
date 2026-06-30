package br.com.toppower.erp_toppower.supplier.exception;

public class DuplicateSupplierCnpjException extends RuntimeException {

    public DuplicateSupplierCnpjException(String cnpj) {
        super("Já existe um fornecedor cadastrado com o CNPJ: " + cnpj);
    }
}
