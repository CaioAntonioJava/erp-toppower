package br.com.toppower.erp_toppower.product.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unidade de medida em que um {@code Product} é comercializado/controlado.
 *
 * <p>Os identificadores correspondem aos <b>códigos oficiais da NF-e</b>
 * (tabela de unidades de medida da SEFAZ), usados no campo {@code uCom}
 * do item da NF-e. O rótulo amigável em português fica no frontend
 * ({@code UNIT_TYPE_LABELS}).</p>
 */
@Schema(name = "UnitType", description = "Unidade de medida do produto (código SEFAZ usado na NF-e).",
        allowableValues = {
                "UN", "MTR", "M2", "M3", "KG", "G", "L", "ML",
                "PC", "CX", "PCT", "RL", "CEM", "PAR", "DZ", "CJ", "FR", "AMP", "BTE"
        })
public enum UnitType {
    UN,     // Unidade
    MTR,    // Metro
    M2,     // Metro quadrado
    M3,     // Metro cúbico
    KG,     // Quilograma
    G,      // Grama
    L,      // Litro
    ML,     // Mililitro
    PC,     // Peça
    CX,     // Caixa
    PCT,    // Pacote
    RL,     // Rolo
    CEM,    // Cento
    PAR,    // Par
    DZ,     // Dúzia
    CJ,     // Conjunto
    FR,     // Fardo
    AMP,    // Ampola
    BTE     // Botão
}