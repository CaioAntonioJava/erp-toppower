package br.com.toppower.erp_toppower.cashflow.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tipo de cliente do movimento de caixa (espelha a distinção
 * Customer/Company usada nos documentos comerciais). Aplica-se apenas
 * a entradas — saídas têm fornecedor como contraparte e ficam fora do
 * filtro quando um filtro de cliente está ativo.
 */
@Schema(name = "CashFlowClientType", description = "Tipo de cliente do movimento de caixa.",
        allowableValues = {"CUSTOMER", "COMPANY"})
public enum CashFlowClientType {
    /** Pessoa física ({@code Customer}, CPF). */
    CUSTOMER,
    /** Pessoa jurídica ({@code Company}, CNPJ). */
    COMPANY
}