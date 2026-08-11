package br.com.toppower.erp_toppower.servicecategory.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ServiceCategoryStatus", description = "Status da categoria de serviço no cadastro.",
        allowableValues = {"ATIVO", "INATIVO"})
public enum ServiceCategoryStatus {
    ATIVO,
    INATIVO
}