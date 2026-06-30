package br.com.toppower.erp_toppower.client.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tipo de pessoa (física ou jurídica) de um cliente.
 *
 * <p>Determina o tipo de documento fiscal esperado no campo
 * {@code taxId} do cliente:</p>
 * <ul>
 *   <li>{@link #FISICA} → CPF (11 dígitos)</li>
 *   <li>{@link #JURIDICA} → CNPJ (14 dígitos)</li>
 * </ul>
 */
@Schema(name = "PersonType", description = "Tipo de pessoa do cliente (física ou jurídica).",
        allowableValues = {"FISICA", "JURIDICA"})
public enum PersonType {
    /** Pessoa Física — usa CPF. */
    FISICA,
    /** Pessoa Jurídica — usa CNPJ. */
    JURIDICA
}
