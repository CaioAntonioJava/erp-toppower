package br.com.toppower.erp_toppower.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status genérico de registro de uma entidade no sistema.
 *
 * <p>Indica se o registro está ativo (pode ser utilizado em operações
 * de negócio — pedidos, notas, etc.) ou inativo. Usado tanto por
 * {@code Company} quanto por {@code Customer}.</p>
 */
@Schema(name = "RegistrationStatus", description = "Status genérico do registro (Company, Customer, ...).",
        allowableValues = {"ATIVO", "INATIVO"})
public enum RegistrationStatus {
    ATIVO,
    INATIVO
}
