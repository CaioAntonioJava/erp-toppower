package br.com.toppower.erp_toppower.product.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Origem da mercadoria na NF-e (campo {@code orig} do ICMS).
 * Valores 0 a 8 conforme tabela do SPED/MF.
 */
@Schema(name = "OrigemProduto", description = "Origem da mercadoria (campo orig da NF-e).",
        allowableValues = {
                "NACIONAL",
                "ESTRANGEIRA_IMPORTACAO_DIRETA",
                "ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO",
                "NACIONAL_IMPORTACAO_SUPERIOR_40",
                "NACIONAL_PROCESSOS_PRODUTIVOS_BASICOS",
                "NACIONAL_IMPORTACAO_SUPERIOR_70",
                "ESTRANGEIRA_IMPORTACAO_DIRETA_SEM_SIMILAR",
                "ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO_SEM_SIMILAR",
                "NACIONAL_IMPORTACAO_ACIMA_70"
        })
public enum OrigemProduto {
    /** 0 — Nacional, exceto as indicadas nos códigos 3, 4, 5 e 8. */
    NACIONAL,
    /** 1 — Estrangeira — Importação direta, com similar nacional (extinta pela Lei 12.865/2013). */
    ESTRANGEIRA_IMPORTACAO_DIRETA,
    /** 2 — Estrangeira — Adquirida no mercado interno, com similar nacional (extinta). */
    ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO,
    /** 3 — Nacional, mercadoria ou bem com conteúdo de importação superior a 40% e inferior ou igual a 70%. */
    NACIONAL_IMPORTACAO_SUPERIOR_40,
    /** 4 — Nacional, produção em conformidade com os Processos Produtivos Básicos (PPB). */
    NACIONAL_PROCESSOS_PRODUTIVOS_BASICOS,
    /** 5 — Nacional, mercadoria ou bem com conteúdo de importação inferior ou igual a 40% (extinto). */
    NACIONAL_IMPORTACAO_SUPERIOR_70,
    /** 6 — Estrangeira — Importação direta, sem similar nacional (constante em lista CAMEX e extinta). */
    ESTRANGEIRA_IMPORTACAO_DIRETA_SEM_SIMILAR,
    /** 7 — Estrangeira — Adquirida no mercado interno, sem similar nacional (constante em lista CAMEX e extinta). */
    ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO_SEM_SIMILAR,
    /** 8 — Nacional, mercadoria ou bem com conteúdo de importação superior a 70%. */
    NACIONAL_IMPORTACAO_ACIMA_70
}