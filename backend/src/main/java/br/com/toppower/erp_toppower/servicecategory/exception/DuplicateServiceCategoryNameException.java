package br.com.toppower.erp_toppower.servicecategory.exception;

public class DuplicateServiceCategoryNameException extends RuntimeException {

    public DuplicateServiceCategoryNameException(String name) {
        super("Já existe uma categoria de serviço cadastrada com o nome: " + name);
    }
}