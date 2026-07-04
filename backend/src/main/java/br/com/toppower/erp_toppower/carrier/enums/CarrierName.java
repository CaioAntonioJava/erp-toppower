package br.com.toppower.erp_toppower.carrier.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Nome padronizado da transportadora. Restringe o cadastro a um conjunto
 * fixo de opções, garantindo consistência nos dados e evitando variações
 * de digitação. Opcional: uma transportadora pode ser cadastrada sem
 * informar o nome (apenas com valor de frete, por exemplo).
 */
@Schema(name = "CarrierName", description = "Nome padronizado da transportadora.",
        allowableValues = {"CORREIOS_SEDEX", "CORREIOS_PAC", "JADLOG", "OUTRAS_TRANSPORTADORAS"})
public enum CarrierName {
    CORREIOS_SEDEX,
    CORREIOS_PAC,
    JADLOG,
    OUTRAS_TRANSPORTADORAS
}