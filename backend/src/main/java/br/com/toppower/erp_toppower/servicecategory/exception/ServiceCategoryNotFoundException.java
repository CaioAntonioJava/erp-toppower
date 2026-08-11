package br.com.toppower.erp_toppower.servicecategory.exception;

public class ServiceCategoryNotFoundException extends RuntimeException {
    public ServiceCategoryNotFoundException(Long id) {
        super("Categoria de serviço não encontrada: " + id);
    }
}