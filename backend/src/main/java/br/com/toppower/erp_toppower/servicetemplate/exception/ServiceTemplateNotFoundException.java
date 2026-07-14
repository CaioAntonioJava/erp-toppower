package br.com.toppower.erp_toppower.servicetemplate.exception;

public class ServiceTemplateNotFoundException extends RuntimeException {
    public ServiceTemplateNotFoundException(Long id) {
        super("Serviço não encontrado: " + id);
    }
}
