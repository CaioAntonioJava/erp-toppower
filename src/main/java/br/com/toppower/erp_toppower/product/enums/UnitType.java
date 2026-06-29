package br.com.toppower.erp_toppower.product.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unidade de medida em que um {@code Product} eh comercializado/controlado.
 */
@Schema(name = "UnitType", description = "Unidade de medida do produto.",
        allowableValues = {"PECAS", "METROS", "BOBINA"})
public enum UnitType {
    PECAS,
    METROS,
    BOBINA
}
