package br.com.toppower.erp_toppower.servicetemplate.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Categoria de serviço no catálogo.
 *
 * <p>Define a classificação do serviço prestado. Novos valores podem ser
 * adicionados conforme a necessidade do negócio.</p>
 */
@Schema(name = "ServiceCategory", description = "Categoria do serviço no catálogo.",
        allowableValues = {"EXECUÇÃO_SPDA"})
public enum ServiceCategory {
    EXECUÇÃO_SPDA
}
